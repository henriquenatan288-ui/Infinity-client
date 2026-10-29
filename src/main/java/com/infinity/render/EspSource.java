package com.infinity.render;

/** Um modulo que desenha caixas/linhas atraves das paredes. */
public interface EspSource {
	boolean active();

	void collect(EspRenderer.Esp esp);
}