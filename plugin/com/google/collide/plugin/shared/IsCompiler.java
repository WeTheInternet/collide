package com.google.collide.plugin.shared;

import com.google.gwt.dev.cfg.ResourceLoader;

public interface IsCompiler {

  ResourceLoader getResourceLoader();

  CompiledDirectory recompile();



}
