package com.witch.render;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/** Renderizador generico de caixas e linhas atraves das paredes. */
public final class EspRenderer {
	private static final RenderPipeline PIPELINE = RenderPipelines.register(
			RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
					.withLocation(Identifier.fromNamespaceAndPath("witch", "pipeline/esp_through_walls"))
					.withDepthStencilState(Optional.empty())
					.build()
	);

	private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f MODEL_OFFSET = new Vector3f();
	private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
	private static final StagedVertexBuffer STAGED =
			new StagedVertexBuffer(() -> "Witch ESP Buffer", RenderType.SMALL_BUFFER_SIZE);

	private static final List<EspSource> SOURCES = new ArrayList<>();
	private static boolean inited = false;

	/** Espessura das linhas em blocos por bloco de distancia (mantem a espessura na tela). */
	private static final float K = 0.0015f;

	private EspRenderer() {}

	public static void clearSources() {
		SOURCES.clear();
	}

	public static void register(EspSource source) {
		SOURCES.add(source);
	}

	public static void init() {
		if (inited) return;
		inited = true;
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(EspRenderer::onRender);
	}

	private static void onRender(LevelRenderContext context) {
		boolean any = false;
		for (EspSource s : SOURCES) {
			if (s.active()) {
				any = true;
				break;
			}
		}
		if (!any) return;
		if (PIPELINE.getVertexFormatBinding(0) == null) return;

		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

		PoseStack pose = context.poseStack();
		Vec3 cam = context.levelState().cameraRenderState.pos;
		// Origem dos tracers: 1 bloco a frente da camera, na direcao real da tela (a rotacao da camera esta
		// na matriz de modelo-vista, nao no poseStack). Se a matriz vier sem rotacao, cai no olhar do jogador.
		Vector3f front = new Matrix4f(RenderSystem.getModelViewMatrixCopy()).invert()
				.transformPosition(new Vector3f(0f, 0f, -1f));
		Vec3 origin;
		if (Math.abs(front.x()) < 1.0E-4f && Math.abs(front.y()) < 1.0E-4f && Math.abs(front.z() + 1f) < 1.0E-4f) {
			origin = mc.player.getEyePosition(pt).add(mc.player.getViewVector(pt));
		} else {
			origin = new Vec3(cam.x + front.x(), cam.y + front.y(), cam.z + front.z());
		}
		pose.pushPose();
		pose.translate(-cam.x, -cam.y, -cam.z);
		Esp esp = new Esp(pose.last().pose(), cam, pt, origin);
		for (EspSource s : SOURCES) {
			if (s.active()) s.collect(esp);
		}
		pose.popPose();

		if (esp.draw == null) return;
		STAGED.upload();
		StagedVertexBuffer.ExecuteInfo info = STAGED.getExecuteInfo(esp.draw);
		if (info != null) {
			execute(mc, info);
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
				() -> "witch esp",
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

	/** Entregue aos modulos: eles chamam box() e tracer() em coordenadas do mundo. */
	public static final class Esp {
		private final Matrix4fc m;
		private final Vec3 cam;
		private final float partial;
		private final Vec3 start;
		private StagedVertexBuffer.Draw draw;
		private VertexConsumer buf;

		private Esp(Matrix4fc m, Vec3 cam, float partial, Vec3 origin) {
			this.m = m;
			this.cam = cam;
			this.partial = partial;
			this.start = origin;
		}

		public float partial() {
			return partial;
		}

		private VertexConsumer b() {
			if (buf == null) {
				var binding = PIPELINE.getVertexFormatBinding(0);
				var primitive = PIPELINE.getPrimitiveTopology();
				var sorting = primitive == PrimitiveTopology.QUADS
						? RenderSystem.getProjectionType().vertexSorting()
						: null;
				draw = STAGED.appendDraw(binding, primitive, sorting);
				buf = STAGED.getVertexBuilder(draw);
			}
			return buf;
		}

		private float dist(double x, double y, double z) {
			double dx = x - cam.x;
			double dy = y - cam.y;
			double dz = z - cam.z;
			return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		}

		/** Caixa com contorno e preenchimento opcional (fillA 0 = so contorno). */
		public void box(double x0, double y0, double z0, double x1, double y1, double z1, int argb, float fillA) {
			VertexConsumer vb = b();
			float h = Math.max(0.02f, K * dist((x0 + x1) * 0.5, (y0 + y1) * 0.5, (z0 + z1) * 0.5));
			float[] line = rgba(argb, 1f);
			if (fillA > 0f) {
				fillBox(vb, m, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1,
						rgba(argb, fillA));
			}
			outline(vb, m, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, line, h);
		}

		/** Linha do centro da tela ate o ponto (x, y, z). */
		public void tracer(double tx, double ty, double tz, int argb) {
			line(start.x, start.y, start.z, tx, ty, tz, argb, 1f);
		}

		/** Caixa so de preenchimento (sem contorno), com transparencia propria. */
		public void solid(double x0, double y0, double z0, double x1, double y1, double z1, int argb, float alpha) {
			fillBox(b(), m, (float) x0, (float) y0, (float) z0, (float) x1, (float) y1, (float) z1, rgba(argb, alpha));
		}

		/** Fita de A ate B (largura cresce com a distancia pra ficar visivel). */
		public void line(double sx, double sy, double sz, double tx, double ty, double tz, int argb, float alpha) {
			float ax = (float) sx, ay = (float) sy, az = (float) sz;
			float bx = (float) tx, by = (float) ty, bz = (float) tz;
			float dx = bx - ax, dy = by - ay, dz = bz - az;
			float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (len < 0.01f) return;
			float nx = dx / len, ny = dy / len, nz = dz / len;

			float rx = 0f, ry = 1f, rz = 0f;
			if (Math.abs(ny) > 0.99f) {
				rx = 1f;
				ry = 0f;
			}
			float ux = ny * rz - nz * ry;
			float uy = nz * rx - nx * rz;
			float uz = nx * ry - ny * rx;
			float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
			if (ul < 1.0E-6f) return;
			ux /= ul;
			uy /= ul;
			uz /= ul;
			float vx = ny * uz - nz * uy;
			float vy = nz * ux - nx * uz;
			float vz = nx * uy - ny * ux;

			float w0 = Math.max(0.004f, K * dist(ax, ay, az));
			float w1 = Math.max(0.004f, K * dist(bx, by, bz));
			float[] c = rgba(argb, alpha);
			VertexConsumer vb = b();
			quad(vb, m, c,
					ax - ux * w0, ay - uy * w0, az - uz * w0,
					bx - ux * w1, by - uy * w1, bz - uz * w1,
					bx + ux * w1, by + uy * w1, bz + uz * w1,
					ax + ux * w0, ay + uy * w0, az + uz * w0);
			quad(vb, m, c,
					ax - vx * w0, ay - vy * w0, az - vz * w0,
					bx - vx * w1, by - vy * w1, bz - vz * w1,
					bx + vx * w1, by + vy * w1, bz + vz * w1,
					ax + vx * w0, ay + vy * w0, az + vz * w0);
		}
	}

	private static float[] rgba(int argb, float alpha) {
		return new float[]{
				((argb >> 16) & 0xFF) / 255f,
				((argb >> 8) & 0xFF) / 255f,
				(argb & 0xFF) / 255f,
				alpha
		};
	}

	private static void fillBox(VertexConsumer buf, Matrix4fc m,
								float bx0, float by0, float bz0, float bx1, float by1, float bz1, float[] c) {
		float e = 0.003f;
		float x0 = bx0 - e, y0 = by0 - e, z0 = bz0 - e;
		float x1 = bx1 + e, y1 = by1 + e, z1 = bz1 + e;

		quad(buf, m, c, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
		quad(buf, m, c, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
		quad(buf, m, c, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
		quad(buf, m, c, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
		quad(buf, m, c, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
		quad(buf, m, c, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
	}

	private static void outline(VertexConsumer buf, Matrix4fc m,
								float x0, float y0, float z0, float x1, float y1, float z1, float[] c, float h) {
		float[] ys = {y0, y1};
		float[] zs = {z0, z1};
		float[] xs = {x0, x1};

		for (float y : ys) {
			for (float z : zs) {
				edge(buf, m, c, h, x0, y, z, x1, y, z, 0f, 1f, 0f, 0f, 0f, 1f);
			}
		}
		for (float x : xs) {
			for (float z : zs) {
				edge(buf, m, c, h, x, y0, z, x, y1, z, 1f, 0f, 0f, 0f, 0f, 1f);
			}
		}
		for (float x : xs) {
			for (float y : ys) {
				edge(buf, m, c, h, x, y, z0, x, y, z1, 1f, 0f, 0f, 0f, 1f, 0f);
			}
		}
	}

	private static void edge(VertexConsumer buf, Matrix4fc m, float[] c, float h,
							 float px, float py, float pz, float qx, float qy, float qz,
							 float ux, float uy, float uz, float vx, float vy, float vz) {
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