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

import org.openapitools.codegen.*;
import org.openapitools.codegen.utils.ModelUtils;

import io.swagger.v3.oas.models.media.*;

import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.utils.ProcessUtils;

import java.io.File;
import java.util.*;

import static org.openapitools.codegen.utils.StringUtils.underscore;

public class DartJaguarClientCodegen extends DartClientCodegen {
    private static final String NULLABLE_FIELDS = "nullableFields";
    private static final String SERIALIZATION_FORMAT = "serialization";
    private static final String IS_FORMAT_JSON = "jsonFormat";
    private static final String IS_FORMAT_PROTO = "protoFormat";
    private static final String CLIENT_NAME = "clientName";

    private static Set<String> modelToIgnore = new HashSet<>();
    private HashMap<String, String> protoTypeMapping = new HashMap<>();

    static {
        modelToIgnore.add("datetime");
        modelToIgnore.add("map");
        modelToIgnore.add("object");
        modelToIgnore.add("list");
        modelToIgnore.add("file");
        modelToIgnore.add("list<int>");
    }

    private static final String SERIALIZATION_JSON = "json";
    private static final String SERIALIZATION_PROTO = "proto";

    private boolean nullableFields = true;
    private String serialization = SERIALIZATION_JSON;

    public DartJaguarClientCodegen() {
        super();
        dartJaguarConstructor();
    }

    @Override
    public String getName() {
        return "dart-jaguar";
    }

    @Override
    public String getHelp() {
        return "Generates a Dart Jaguar client library.";
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
    public void processOpts() {
        dartJaguarProcessOps();
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        return dartJaguarPostProcessModels(objs);
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        return  dartJaguarPostProcessOperationsWithModels(objs, allModels);
    }
}
