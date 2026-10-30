package com.witch.render;

import com.witch.module.XRayModule;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class XRayRenderer {
	private static final RenderPipeline PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("witch", "pipeline/xray_through_walls"))
					.withDepthStencilState(Optional.empty())
					.build()
	);

	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private static final StagedVertexBuffer STAGED =
			new StagedVertexBuffer(() -> "Witch XRay Buffer", RenderType.SMALL_BUFFER_SIZE);

	private static final float HALF = 0.025f;

	private XRayRenderer() {}

	public static void init() {
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(XRayRenderer::onRender);
	}

	public static void close() {
		STAGED.close();
	}

	private static void onRender(LevelRenderContext context) {
		if (!XRayModule.active()) return;
		List<int[]> blocks = XRayModule.blocks();
		if (blocks.isEmpty()) return;

		var formatBinding = PIPELINE.getVertexFormatBinding(0);
		if (formatBinding == null) return;
		var primitive = PIPELINE.getPrimitiveTopology();
		var sorting = primitive == PrimitiveTopology.QUADS
				? RenderSystem.getProjectionType().vertexSorting()
				: null;
		StagedVertexBuffer.Draw draw = STAGED.appendDraw(formatBinding, primitive, sorting);

		float fillA = XRayModule.fillAlpha();

		PoseStack pose = context.poseStack();
		Vec3 cam = context.levelState().cameraRenderState.pos;
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		VertexConsumer builder = STAGED.getVertexBuilder(draw);
		Matrix4fc matrix = pose.last().pose();
		for (int[] b : blocks) {
			int argb = b.length > 3 ? b[3] : 0xFF8A2BE2;
			float[] line = {
					((argb >> 16) & 0xFF) / 255f,
					((argb >> 8) & 0xFF) / 255f,
					(argb & 0xFF) / 255f,
					1f
			};
			if (fillA > 0f) {
				float[] fill = {line[0], line[1], line[2], fillA};
				fillBox(builder, matrix, b[0], b[1], b[2], fill);
			}
			outline(builder, matrix, b[0], b[1], b[2], line);
		}
		pose.popPose();

		STAGED.upload();
		StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(draw);
		if (info != null) {
			execute(Minecraft.getInstance(), info);
		}
		STAGED.endFrame();
	}

	private static void execute(Minecraft client, StagedVertexBuffer.ExecuteInfo info) {
		GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
				RenderSystem.getModelViewMatrixCopy(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

		RenderTarget mainTarget = client.gameRenderer.mainRenderTarget();
		GpuTextureView colorTexture = mainTarget.getColorTextureView();
		if (colorTexture == null) return;

		try (RenderPass renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
				() -> "witch xray",
				colorTexture,
				Optional.empty(),
				mainTarget.getDepthTextureView(),
				OptionalDouble.empty())) {
			renderPass.setPipeline(PIPELINE);
			RenderSystem.bindDefaultUniforms(renderPass);
			renderPass.setUniform("DynamicTransforms", dynamicTransforms);
			renderPass.setVertexBuffer(0, info.vertexBuffer().slice());
			renderPass.setIndexBuffer(info.indexBuffer(), info.indexType());
			renderPass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
		}
	}

	private static void fillBox(VertexConsumer buf, Matrix4fc m, int bx, int by, int bz, float[] c) {
		float e = 0.003f;
		float x0 = bx - e, y0 = by - e, z0 = bz - e;
		float x1 = bx + 1 + e, y1 = by + 1 + e, z1 = bz + 1 + e;

		quad(buf, m, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(buf, m, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(buf, m, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		quad(buf, m, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(buf, m, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		quad(buf, m, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	/** As 12 arestas do bloco, cada uma como duas faixas finas cruzadas. */
	private static void outline(VertexConsumer buf, Matrix4fc m, int bx, int by, int bz, float[] c) {
		float x0 = bx, y0 = by, z0 = bz;
		float x1 = bx + 1f, y1 = by + 1f, z1 = bz + 1f;
		float[] ys = {y0, y1};
		float[] zs = {z0, z1};
		float[] xs = {x0, x1};

		for (float y : ys) {
			for (float z : zs) {
				edge(buf, m, c, x0, y, z, x1, y, z, 0f, 1f, 0f, 0f, 0f, 1f);
			}
		}
		for (float x : xs) {
			for (float z : zs) {
				edge(buf, m, c, x, y0, z, x, y1, z, 1f, 0f, 0f, 0f, 0f, 1f);
			}
		}
		for (float x : xs) {
			for (float y : ys) {
				edge(buf, m, c, x, y, z0, x, y, z1, 1f, 0f, 0f, 0f, 1f, 0f);
			}
		}
	}

	private static void edge(VertexConsumer buf, Matrix4fc m, float[] c,
							 float px, float py, float pz, float qx, float qy, float qz,
							 float ux, float uy, float uz, float vx, float vy, float vz) {
		float h = HALF;
		quad(buf, m, c,
				px - ux * h, py - uy * h, pz - uz * h,
				qx - ux * h, qy - uy * h, qz - uz * h,
				qx + ux * h, qy + uy * h, qz + uz * h,
				px + ux * h, py + uy * h, pz + uz * h);
		quad(buf, m, c,
				px - vx * h, py - vy * h, pz - vz * h,
				qx - vx * h, qy - vy * h, qz - vz * h,
				qx + vx * h, qy + vy * h, qz + vz * h,
				px + vx * h, py + vy * h, pz + vz * h);
	}

	/** Escreve a face nos dois sentidos, assim nao importa se o pipeline descarta faces de tras. */
	private static void quad(VertexConsumer buf, Matrix4fc m, float[] c,
							 float ax, float ay, float az, float bx, float by, float bz,
							 float cx, float cy, float cz, float dx, float dy, float dz) {
		v(buf, m, c, ax, ay, az);
		v(buf, m, c, bx, by, bz);
		v(buf, m, c, cx, cy, cz);
		v(buf, m, c, dx, dy, dz);

		v(buf, m, c, dx, dy, dz);
		v(buf, m, c, cx, cy, cz);
		v(buf, m, c, bx, by, bz);
		v(buf, m, c, ax, ay, az);
	}

	private static void v(VertexConsumer buf, Matrix4fc m, float[] c, float x, float y, float z) {
		buf.addVertex(m, x, y, z).setColor(c[0], c[1], c[2], c[3]);
	}
}