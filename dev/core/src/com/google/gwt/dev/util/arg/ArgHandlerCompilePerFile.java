/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.util.arg;

import com.google.gwt.util.tools.ArgHandlerFlag;

/**
 * Whether recompiles should process only changed files and construct JS output by
 * linking old and new JS on a per class basis.
 */
public class ArgHandlerCompilePerFile extends ArgHandlerFlag {

  private final OptionCompilePerFile option;

  public ArgHandlerCompilePerFile(OptionCompilePerFile option) {
    this.option = option;
  }

  @Override
  public String getPurposeSnippet() {
    return "Compile, link and recompile on a per-file basis.";
  }

  @Override
  public String getLabel() {
    return "compilePerFile";
  }

  @Override
  public boolean setFlag(boolean value) {
    option.setCompilePerFile(value);
    return true;
  }

  @Override
  public boolean isExperimental() {
    return true;
  }

  @Override
  public boolean getDefaultValue() {
    return option.shouldCompilePerFile();
  }
}
