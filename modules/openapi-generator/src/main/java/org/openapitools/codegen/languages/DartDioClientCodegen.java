/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.CliOption;
import org.openapitools.codegen.CodegenConstants;
import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.SupportingFile;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.models.media.Schema;

import static org.openapitools.codegen.utils.StringUtils.camelize;
import static org.openapitools.codegen.utils.StringUtils.underscore;

public class DartDioClientCodegen extends DartClientCodegen {
    private static final Logger LOGGER = LoggerFactory.getLogger(DartDioClientCodegen.class);


    private static final String IS_FORMAT_JSON = "jsonFormat";
    private static final String CLIENT_NAME = "clientName";

    private static Set<String> modelToIgnore = new HashSet<>();

    static {
        modelToIgnore.add("datetime");
        modelToIgnore.add("map");
        modelToIgnore.add("object");
        modelToIgnore.add("list");
        modelToIgnore.add("file");
        modelToIgnore.add("uint8list");
    }

    private static final String SERIALIZATION_JSON = "json";

    private boolean nullableFields = true;

    public DartDioClientCodegen() {
        super();
        dartDioConstrutor();
    }

    @Override
    public String getName() {
        return "dart-dio";
    }

    @Override
    public String getHelp() {
        return "Generates a Dart Dio client library.";
    }

    @Override
    public String toDefaultValue(Schema p) {
        if (ModelUtils.isMapSchema(p)) {
            return "const {}";
        } else if (ModelUtils.isArraySchema(p)) {
            return "const []";
        }
        return super.toDefaultValue(p);
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        if (name.length() == 0) {
            return "empty";
        }
        if ("number".equalsIgnoreCase(datatype) ||
            "int".equalsIgnoreCase(datatype)) {
            name = "Number" + name;
        }
        name = camelize(name, true);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }
        return name;
    }

    @Override
    public void processOpts() {
        dartDioProcessOpts();
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        return dartDioPostProcessModels(objs);
    }

    @Override
    public void dartDioPostProcessModelProperty(CodegenModel model, CodegenProperty property) {
        dartDioPostProcessModelProperty(model, property);
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        return dartDioPostProcessOperationsWithModels(objs, allModels);
    }
}
