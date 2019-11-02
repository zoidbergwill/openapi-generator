/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
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

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.utils.ModelUtils;
import org.openapitools.codegen.utils.ProcessUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.openapitools.codegen.utils.StringUtils.camelize;
import static org.openapitools.codegen.utils.StringUtils.underscore;

public class DartClientCodegen extends DefaultCodegen implements CodegenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DartClientCodegen.class);

    public static final String BROWSER_CLIENT = "browserClient";
    public static final String PUB_NAME = "pubName";
    public static final String PUB_VERSION = "pubVersion";
    public static final String PUB_DESCRIPTION = "pubDescription";
    public static final String USE_ENUM_EXTENSION = "useEnumExtension";
    public static final String SUPPORT_DART2 = "supportDart2";
    public static final String NULLABLE_FIELDS = "nullableFields";

    // -- dart jaguar
    private static final String SERIALIZATION_FORMAT = "serialization";
    private static final String IS_FORMAT_JSON = "jsonFormat";
    private static final String IS_FORMAT_PROTO = "protoFormat";
    private static final String CLIENT_NAME = "clientName";

    private static Set<String> modelToIgnore = new HashSet<>();
    private HashMap<String, String> protoTypeMapping = new HashMap<>();

    // -- dart dio
    //private static final String IS_FORMAT_JSON = "jsonFormat";
    //private static final String CLIENT_NAME = "clientName";

    static {
        modelToIgnore.add("datetime");
        modelToIgnore.add("map");
        modelToIgnore.add("object");
        modelToIgnore.add("list");
        modelToIgnore.add("file");
        modelToIgnore.add("uint8list");
    }

    private static final String SERIALIZATION_JSON = "json";
    private static final String SERIALIZATION_PROTO = "proto";

    private boolean nullableFields = true;
    private String serialization = SERIALIZATION_JSON;

    // -- end


    protected boolean browserClient = true;
    protected String pubName = "openapi";
    protected String pubVersion = "1.0.0";
    protected String pubDescription = "OpenAPI API client";
    protected boolean useEnumExtension = false;
    protected String sourceFolder = "";
    protected String apiDocPath = "docs" + File.separator;
    protected String modelDocPath = "docs" + File.separator;
    protected String apiTestPath = "test" + File.separator;
    protected String modelTestPath = "test" + File.separator;

    public static final String HTTP = "http";
    public static final String DIO = "dio";
    public static final String JAGUAR = "jaguar";

    public DartClientCodegen() {
        super();

        // clear import mapping (from default generator) as dart does not use it
        // at the moment
        importMapping.clear();

        outputFolder = "generated-code/dart";
        modelTemplateFiles.put("model.mustache", ".dart");
        apiTemplateFiles.put("api.mustache", ".dart");
        embeddedTemplateDir = templateDir = "dart2";
        apiPackage = "lib.api";
        modelPackage = "lib.model";
        modelDocTemplateFiles.put("object_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        modelTestTemplateFiles.put("model_test.mustache", ".dart");
        apiTestTemplateFiles.put("api_test.mustache", ".dart");

        setReservedWordsLowerCase(
                Arrays.asList(
                        "abstract", "as", "assert", "async", "async*", "await",
                        "break", "case", "catch", "class", "const", "continue",
                        "default", "deferred", "do", "dynamic", "else", "enum",
                        "export", "external", "extends", "factory", "false", "final",
                        "finally", "for", "get", "if", "implements", "import", "in",
                        "is", "library", "new", "null", "operator", "part", "rethrow",
                        "return", "set", "static", "super", "switch", "sync*", "this",
                        "throw", "true", "try", "typedef", "var", "void", "while",
                        "with", "yield", "yield*", "hide", "interface", "mixin", "on",
                        "show", "async")
        );

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList(
                        "String",
                        "bool",
                        "int",
                        "num",
                        "double")
        );
        instantiationTypes.put("array", "List");
        instantiationTypes.put("map", "Map");

        typeMapping = new HashMap<String, String>();
        typeMapping.put("Array", "List");
        typeMapping.put("array", "List");
        typeMapping.put("List", "List");
        typeMapping.put("boolean", "bool");
        typeMapping.put("string", "String");
        typeMapping.put("char", "String");
        typeMapping.put("int", "int");
        typeMapping.put("long", "int");
        typeMapping.put("short", "int");
        typeMapping.put("number", "num");
        typeMapping.put("float", "double");
        typeMapping.put("double", "double");
        typeMapping.put("object", "Object");
        typeMapping.put("integer", "int");
        typeMapping.put("Date", "DateTime");
        typeMapping.put("date", "DateTime");
        typeMapping.put("File", "MultipartFile");
        typeMapping.put("binary", "MultipartFile");
        typeMapping.put("UUID", "String");
        typeMapping.put("URI", "String");
        typeMapping.put("ByteArray", "String");

        cliOptions.add(new CliOption(BROWSER_CLIENT, "Is the client browser based (for Dart 1.x only)"));
        cliOptions.add(new CliOption(PUB_NAME, "Name in generated pubspec"));
        cliOptions.add(new CliOption(PUB_VERSION, "Version in generated pubspec"));
        cliOptions.add(new CliOption(PUB_DESCRIPTION, "Description in generated pubspec"));
        cliOptions.add(new CliOption(USE_ENUM_EXTENSION, "Allow the 'x-enum-values' extension for enums"));
        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, "Source folder for generated code"));
        cliOptions.add(CliOption.newBoolean(SUPPORT_DART2, "Support Dart 2.x (Dart 1.x support has been deprecated)").defaultValue(Boolean.TRUE.toString()));

        supportedLibraries.put(HTTP, "http: https://pub.dev/packages/http");
        supportedLibraries.put(DIO, "dio: https://pub.dev/packages/dio");
        supportedLibraries.put(JAGUAR, "jaguar: https://pub.dev/packages/jaguar");

        CliOption libraryOption = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        libraryOption.setEnum(supportedLibraries);
        // set http as the default
        libraryOption.setDefault(HTTP);
        cliOptions.add(libraryOption);
        setLibrary(HTTP);
    }

    public void dartDioConstrutor() {
        browserClient = false;
        outputFolder = "generated-code/dart-dio";
        embeddedTemplateDir = templateDir = "dart-dio";

        //no tests at this time
        modelTestTemplateFiles.clear();
        apiTestTemplateFiles.clear();
    }

    public void dartJaguarConstructor() {
        browserClient = false;
        outputFolder = "generated-code/dart-jaguar";
        embeddedTemplateDir = templateDir = "dart-jaguar";

        cliOptions.add(new CliOption(NULLABLE_FIELDS, "Is the null fields should be in the JSON payload"));
        cliOptions.add(new CliOption(SERIALIZATION_FORMAT, "Choose serialization format JSON or PROTO is supported"));

        typeMapping.put("file", "List<int>");
        typeMapping.put("binary", "List<int>");

        protoTypeMapping.put("Array", "repeated");
        protoTypeMapping.put("array", "repeated");
        protoTypeMapping.put("List", "repeated");
        protoTypeMapping.put("boolean", "bool");
        protoTypeMapping.put("string", "string");
        protoTypeMapping.put("char", "string");
        protoTypeMapping.put("int", "int32");
        protoTypeMapping.put("long", "int64");
        protoTypeMapping.put("short", "int32");
        protoTypeMapping.put("number", "double");
        protoTypeMapping.put("float", "float");
        protoTypeMapping.put("double", "double");
        protoTypeMapping.put("object", "google.protobuf.Any");
        protoTypeMapping.put("integer", "int32");
        protoTypeMapping.put("Date", "google.protobuf.Timestamp");
        protoTypeMapping.put("date", "google.protobuf.Timestamp");
        protoTypeMapping.put("File", "bytes");
        protoTypeMapping.put("file", "bytes");
        protoTypeMapping.put("binary", "bytes");
        protoTypeMapping.put("UUID", "string");
        protoTypeMapping.put("URI", "string");
        protoTypeMapping.put("ByteArray", "bytes");
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "dart";
    }

    @Override
    public String getHelp() {
        return "Generates a Dart client library.";
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (DIO.equals(getLibrary())) {
            dartDioConstrutor();
            dartDioProcessOpts();
        } else if (JAGUAR.equals(getLibrary())) {
            dartJaguarConstructor();
            dartJaguarProcessOps();
        } else { // default
            dartHttpProcessOps();
        }
    }

    public void dartJaguarProcessOps() {

        if (additionalProperties.containsKey(NULLABLE_FIELDS)) {
            nullableFields = convertPropertyToBooleanAndWriteBack(NULLABLE_FIELDS);
        } else {
            //not set, use to be passed to template
            additionalProperties.put(NULLABLE_FIELDS, nullableFields);
        }

        if (additionalProperties.containsKey(SERIALIZATION_FORMAT)) {
            serialization = ((String) additionalProperties.get(SERIALIZATION_FORMAT));
            boolean isProto = serialization.equalsIgnoreCase(SERIALIZATION_PROTO);
            additionalProperties.put(IS_FORMAT_JSON, serialization.equalsIgnoreCase(SERIALIZATION_JSON));
            additionalProperties.put(IS_FORMAT_PROTO, isProto);

            modelTemplateFiles.put("model.mustache", isProto ? ".proto" : ".dart");

        } else {
            //not set, use to be passed to template
            additionalProperties.put(IS_FORMAT_JSON, true);
            additionalProperties.put(IS_FORMAT_PROTO, false);
        }

        if (additionalProperties.containsKey(PUB_NAME)) {
            this.setPubName((String) additionalProperties.get(PUB_NAME));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_NAME, pubName);
        }
        additionalProperties.put(CLIENT_NAME, org.openapitools.codegen.utils.StringUtils.camelize(pubName));

        if (additionalProperties.containsKey(PUB_VERSION)) {
            this.setPubVersion((String) additionalProperties.get(PUB_VERSION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_VERSION, pubVersion);
        }

        if (additionalProperties.containsKey(PUB_DESCRIPTION)) {
            this.setPubDescription((String) additionalProperties.get(PUB_DESCRIPTION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_DESCRIPTION, pubDescription);
        }

        if (additionalProperties.containsKey(USE_ENUM_EXTENSION)) {
            this.setUseEnumExtension(convertPropertyToBooleanAndWriteBack(USE_ENUM_EXTENSION));
        } else {
            // Not set, use to be passed to template.
            additionalProperties.put(USE_ENUM_EXTENSION, useEnumExtension);
        }

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        }

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        final String libFolder = sourceFolder + File.separator + "lib";
        supportingFiles.add(new SupportingFile("pubspec.mustache", "", "pubspec.yaml"));
        supportingFiles.add(new SupportingFile("analysis_options.mustache", "", "analysis_options.yaml"));
        supportingFiles.add(new SupportingFile("apilib.mustache", libFolder, "api.dart"));

        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("travis.mustache", "", ".travis.yml"));

        final String authFolder = sourceFolder + File.separator + "lib" + File.separator + "auth";
        supportingFiles.add(new SupportingFile("auth/api_key_auth.mustache", authFolder, "api_key_auth.dart"));
        supportingFiles.add(new SupportingFile("auth/basic_auth.mustache", authFolder, "basic_auth.dart"));
        supportingFiles.add(new SupportingFile("auth/oauth.mustache", authFolder, "oauth.dart"));
        supportingFiles.add(new SupportingFile("auth/auth.mustache", authFolder, "auth.dart"));
    }

    public void dartDioProcessOpts() {
        if (StringUtils.isEmpty(System.getenv("DART_POST_PROCESS_FILE"))) {
            LOGGER.info("Environment variable DART_POST_PROCESS_FILE not defined so the Dart code may not be properly formatted. To define it, try `export DART_POST_PROCESS_FILE=\"/usr/local/bin/dartfmt -w\"` (Linux/Mac)");
            LOGGER.info("NOTE: To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        }

        if (additionalProperties.containsKey(NULLABLE_FIELDS)) {
            nullableFields = convertPropertyToBooleanAndWriteBack(NULLABLE_FIELDS);
        } else {
            //not set, use to be passed to template
            additionalProperties.put(NULLABLE_FIELDS, nullableFields);
        }

        additionalProperties.put(IS_FORMAT_JSON, true);

        if (additionalProperties.containsKey(PUB_NAME)) {
            this.setPubName((String) additionalProperties.get(PUB_NAME));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_NAME, pubName);
        }

        if (!additionalProperties.containsKey(CLIENT_NAME)) {
            additionalProperties.put(CLIENT_NAME, org.openapitools.codegen.utils.StringUtils.camelize(pubName));
        }

        if (additionalProperties.containsKey(PUB_VERSION)) {
            this.setPubVersion((String) additionalProperties.get(PUB_VERSION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_VERSION, pubVersion);
        }

        if (additionalProperties.containsKey(PUB_DESCRIPTION)) {
            this.setPubDescription((String) additionalProperties.get(PUB_DESCRIPTION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_DESCRIPTION, pubDescription);
        }

        if (additionalProperties.containsKey(USE_ENUM_EXTENSION)) {
            this.setUseEnumExtension(convertPropertyToBooleanAndWriteBack(USE_ENUM_EXTENSION));
        } else {
            // Not set, use to be passed to template.
            additionalProperties.put(USE_ENUM_EXTENSION, useEnumExtension);
        }

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        }

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        final String libFolder = sourceFolder + File.separator + "lib";
        supportingFiles.add(new SupportingFile("pubspec.mustache", "", "pubspec.yaml"));
        supportingFiles.add(new SupportingFile("analysis_options.mustache", "", "analysis_options.yaml"));
        supportingFiles.add(new SupportingFile("apilib.mustache", libFolder, "api.dart"));
        supportingFiles.add(new SupportingFile("serializers.mustache", libFolder, "serializers.dart"));

        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));

    }

    public void dartHttpProcessOps() {
        if (StringUtils.isEmpty(System.getenv("DART_POST_PROCESS_FILE"))) {
            LOGGER.info("Environment variable DART_POST_PROCESS_FILE not defined so the Dart code may not be properly formatted. To define it, try `export DART_POST_PROCESS_FILE=\"/usr/local/bin/dartfmt -w\"` (Linux/Mac)");
            LOGGER.info("NOTE: To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        }

        if (additionalProperties.containsKey(BROWSER_CLIENT)) {
            this.setBrowserClient(convertPropertyToBooleanAndWriteBack(BROWSER_CLIENT));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(BROWSER_CLIENT, browserClient);
        }

        if (additionalProperties.containsKey(PUB_NAME)) {
            this.setPubName((String) additionalProperties.get(PUB_NAME));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_NAME, pubName);
        }

        if (additionalProperties.containsKey(PUB_VERSION)) {
            this.setPubVersion((String) additionalProperties.get(PUB_VERSION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_VERSION, pubVersion);
        }

        if (additionalProperties.containsKey(PUB_DESCRIPTION)) {
            this.setPubDescription((String) additionalProperties.get(PUB_DESCRIPTION));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(PUB_DESCRIPTION, pubDescription);
        }

        if (additionalProperties.containsKey(USE_ENUM_EXTENSION)) {
            this.setUseEnumExtension(convertPropertyToBooleanAndWriteBack(USE_ENUM_EXTENSION));
        } else {
            // Not set, use to be passed to template.
            additionalProperties.put(USE_ENUM_EXTENSION, useEnumExtension);
        }

        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            this.setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        }

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        final Object isSupportDart2 = additionalProperties.get(SUPPORT_DART2);
        if (Boolean.FALSE.equals(isSupportDart2) || (isSupportDart2 instanceof String && !Boolean.parseBoolean((String) isSupportDart2))) {
            // dart 1.x
            LOGGER.info("Dart version: 1.x");
            supportingFiles.add(new SupportingFile("analysis_options.mustache", "", ".analysis_options"));
        } else {
            // dart 2.x
            LOGGER.info("Dart version: 2.x");
            // check to not overwrite a custom templateDir
            if (templateDir == null) {
                embeddedTemplateDir = templateDir = "dart2";
            }
        }

        final String libFolder = sourceFolder + File.separator + "lib";
        supportingFiles.add(new SupportingFile("pubspec.mustache", "", "pubspec.yaml"));
        supportingFiles.add(new SupportingFile("api_client.mustache", libFolder, "api_client.dart"));
        supportingFiles.add(new SupportingFile("api_exception.mustache", libFolder, "api_exception.dart"));
        supportingFiles.add(new SupportingFile("api_helper.mustache", libFolder, "api_helper.dart"));
        supportingFiles.add(new SupportingFile("apilib.mustache", libFolder, "api.dart"));

        final String authFolder = sourceFolder + File.separator + "lib" + File.separator + "auth";
        supportingFiles.add(new SupportingFile("auth/authentication.mustache", authFolder, "authentication.dart"));
        supportingFiles.add(new SupportingFile("auth/http_basic_auth.mustache", authFolder, "http_basic_auth.dart"));
        supportingFiles.add(new SupportingFile("auth/api_key_auth.mustache", authFolder, "api_key_auth.dart"));
        supportingFiles.add(new SupportingFile("auth/oauth.mustache", authFolder, "oauth.dart"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("travis.mustache", "", ".travis.yml"));
    }

    @Override
    public String escapeReservedWord(String name) {
        return name + "_";
    }

    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return outputFolder + File.separator + sourceFolder + File.separator + modelPackage().replace('.', File.separatorChar);
    }

    @Override
    public String apiTestFileFolder() {
        return outputFolder + File.separator + apiTestPath.replace('/', File.separatorChar);
    }

    @Override
    public String modelTestFileFolder() {
        return outputFolder + File.separator + modelTestPath.replace('/', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return outputFolder + File.separator + apiDocPath.replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return outputFolder + File.separator + modelDocPath.replace('/', File.separatorChar);
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_");

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = camelize(name, true);

        if (name.matches("^\\d.*")) {
            name = "n" + name;
        }

        if (isReservedWord(name)) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            LOGGER.warn(name + " (reserved word) cannot be used as model filename. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        if (name.matches("^\\d.*")) {
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(name);
    }

    @Override
    public String toModelFilename(String name) {
        return underscore(toModelName(name));
    }

    @Override
    public String toApiFilename(String name) {
        return underscore(toApiName(name));
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiFilename(name) + "_test";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelFilename(name) + "_test";
    }

    @Override
    public String toDefaultValue(Schema schema) {
        if (ModelUtils.isMapSchema(schema)) {
            return "{}";
        } else if (ModelUtils.isArraySchema(schema)) {
            return "[]";
        }

        if (schema.getDefault() != null) {
            if (ModelUtils.isStringSchema(schema)) {
                return "\"" + schema.getDefault().toString().replaceAll("\"", "\\\"") + "\"";
            }
            return schema.getDefault().toString();
        } else {
            return "null";
        }
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        if (ModelUtils.isArraySchema(p)) {
            ArraySchema ap = (ArraySchema) p;
            Schema inner = ap.getItems();
            return getSchemaType(p) + "<" + getTypeDeclaration(inner) + ">";
        } else if (ModelUtils.isMapSchema(p)) {
            Schema inner = ModelUtils.getAdditionalProperties(p);

            return getSchemaType(p) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(p);
    }

    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);
        String type = null;
        if (typeMapping.containsKey(openAPIType)) {
            type = typeMapping.get(openAPIType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            }
        } else {
            type = openAPIType;
        }
        return toModelName(type);
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        return postProcessModelsEnum(objs);
    }

    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        List<Object> models = (List<Object>) objs.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            boolean succes = buildEnumFromVendorExtension(cm) ||
                    buildEnumFromValues(cm);
            for (CodegenProperty var : cm.vars) {
                updateCodegenPropertyEnum(var);
            }
        }
        return objs;
    }

    /**
     * Builds the set of enum members from their declared value.
     *
     * @return {@code true} if the enum was built
     */
    private boolean buildEnumFromValues(CodegenModel cm) {
        if (!cm.isEnum || cm.allowableValues == null) {
            return false;
        }
        Map<String, Object> allowableValues = cm.allowableValues;
        List<Object> values = (List<Object>) allowableValues.get("values");
        List<Map<String, String>> enumVars =
                new ArrayList<Map<String, String>>();
        String commonPrefix = findCommonPrefixOfVars(values);
        int truncateIdx = commonPrefix.length();
        for (Object value : values) {
            Map<String, String> enumVar = new HashMap<String, String>();
            String enumName;
            if (truncateIdx == 0) {
                enumName = value.toString();
            } else {
                enumName = value.toString().substring(truncateIdx);
                if ("".equals(enumName)) {
                    enumName = value.toString();
                }
            }
            enumVar.put("name", toEnumVarName(enumName, cm.dataType));
            enumVar.put("value", toEnumValue(value.toString(), cm.dataType));
            enumVars.add(enumVar);
        }
        cm.allowableValues.put("enumVars", enumVars);
        return true;
    }

    /**
     * Builds the set of enum members from a vendor extension.
     *
     * @return {@code true} if the enum was built
     */
    private boolean buildEnumFromVendorExtension(CodegenModel cm) {
        if (!cm.isEnum || cm.allowableValues == null ||
                !useEnumExtension ||
                !cm.vendorExtensions.containsKey("x-enum-values")) {
            return false;
        }
        Object extension = cm.vendorExtensions.get("x-enum-values");
        List<Map<String, Object>> values =
                (List<Map<String, Object>>) extension;
        List<Map<String, String>> enumVars =
                new ArrayList<Map<String, String>>();
        for (Map<String, Object> value : values) {
            Map<String, String> enumVar = new HashMap<String, String>();
            String name = camelize((String) value.get("identifier"), true);
            if (isReservedWord(name)) {
                name = escapeReservedWord(name);
            }
            enumVar.put("name", name);
            enumVar.put("value", toEnumValue(
                    value.get("numericValue").toString(), cm.dataType));
            if (value.containsKey("description")) {
                enumVar.put("description", value.get("description").toString());
            }
            enumVars.add(enumVar);
        }
        cm.allowableValues.put("enumVars", enumVars);
        return true;
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (value.length() == 0) {
            return "empty";
        }
        String var = value.replaceAll("\\W+", "_");
        if ("number".equalsIgnoreCase(datatype) ||
                "int".equalsIgnoreCase(datatype)) {
            var = "Number" + var;
        }
        return escapeReservedWord(camelize(var, true));
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("number".equalsIgnoreCase(datatype) ||
                "int".equalsIgnoreCase(datatype)) {
            return value;
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    @Override
    public String toOperationId(String operationId) {
        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, true);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return camelize(operationId, true);
    }

    public void setBrowserClient(boolean browserClient) {
        this.browserClient = browserClient;
    }

    public void setPubName(String pubName) {
        this.pubName = pubName;
    }

    public void setPubVersion(String pubVersion) {
        this.pubVersion = pubVersion;
    }

    public void setPubDescription(String pubDescription) {
        this.pubDescription = pubDescription;
    }

    public void setUseEnumExtension(boolean useEnumExtension) {
        this.useEnumExtension = useEnumExtension;
    }

    public void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    @Override
    public void postProcessFile(File file, String fileType) {
        if (file == null) {
            return;
        }

        String dartPostProcessFile = System.getenv("DART_POST_PROCESS_FILE");
        if (StringUtils.isEmpty(dartPostProcessFile)) {
            return; // skip if DART_POST_PROCESS_FILE env variable is not defined
        }

        // only procees the following type (or we can simply rely on the file extension to check if it's a Dart file)
        Set<String> supportedFileType = new HashSet<String>(
                Arrays.asList(
                        "supporting-mustache",
                        "model-test",
                        "model",
                        "api-test",
                        "api"));
        if (!supportedFileType.contains(fileType)) {
            return;
        }

        // only process files with dart extension
        if ("dart".equals(FilenameUtils.getExtension(file.toString()))) {
            // currently only support "dartfmt -w yourcode.dart"
            String command = dartPostProcessFile + " " + file.toString();
            try {
                Process p = Runtime.getRuntime().exec(command);
                int exitValue = p.waitFor();
                if (exitValue != 0) {
                    LOGGER.error("Error running the command ({}). Exit code: {}", command, exitValue);
                } else {
                    LOGGER.info("Successfully executed: " + command);
                }
            } catch (Exception e) {
                LOGGER.error("Error running the command ({}). Exception: {}", command, e.getMessage());
            }
        }
    }

    public Map<String, Object> dartDioPostProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");

        Set<String> modelImports = new HashSet<>();
        Set<String> fullImports = new HashSet<>();

        for (CodegenOperation op : operationList) {
            op.httpMethod = op.httpMethod.toLowerCase(Locale.ROOT);
            boolean isJson = true; //default to JSON
            boolean isForm = false;
            boolean isMultipart = false;
            if (op.consumes != null) {
                for (Map<String, String> consume : op.consumes) {
                    if (consume.containsKey("mediaType")) {
                        String type = consume.get("mediaType");
                        isJson = type.equalsIgnoreCase("application/json");
                        isForm = type.equalsIgnoreCase("application/x-www-form-urlencoded");
                        isMultipart = type.equalsIgnoreCase("multipart/form-data");
                        break;
                    }
                }
            }

            for (CodegenParameter param : op.bodyParams) {
                if (param.baseType != null && param.baseType.equalsIgnoreCase("Uint8List") && isMultipart) {
                    param.baseType = "MultipartFile";
                    param.dataType = "MultipartFile";
                }
            }

            op.vendorExtensions.put("isJson", isJson);
            op.vendorExtensions.put("isForm", isForm);
            op.vendorExtensions.put("isMultipart", isMultipart);

            Set<String> imports = new HashSet<>();
            for (String item : op.imports) {
                if (!modelToIgnore.contains(item.toLowerCase(Locale.ROOT))) {
                    imports.add(underscore(item));
                } else if (item.equalsIgnoreCase("Uint8List")) {
                    fullImports.add("dart:typed_data");
                }
            }
            modelImports.addAll(imports);
            op.imports = imports;
        }

        objs.put("modelImports", modelImports);
        objs.put("fullImports", fullImports);

        return objs;
    }

    public Map<String, Object> dartJaguarPostProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");

        Set<String> modelImports = new HashSet<>();
        Set<String> fullImports = new HashSet<>();

        for (CodegenOperation op : operationList) {
            op.httpMethod = StringUtils.capitalize(op.httpMethod.toLowerCase(Locale.ROOT));
            boolean isJson = true; //default to JSON
            boolean isForm = false;
            boolean isProto = false;
            boolean isMultipart = false;
            if (op.consumes != null) {
                for (Map<String, String> consume : op.consumes) {
                    if (consume.containsKey("mediaType")) {
                        String type = consume.get("mediaType");
                        isJson = type.equalsIgnoreCase("application/json");
                        isProto = type.equalsIgnoreCase("application/octet-stream");
                        isForm = type.equalsIgnoreCase("application/x-www-form-urlencoded");
                        isMultipart = type.equalsIgnoreCase("multipart/form-data");
                        break;
                    }
                }
            }

            for (CodegenParameter param : op.allParams) {
                if (param.baseType != null && param.baseType.equalsIgnoreCase("List<int>") && isMultipart) {
                    param.baseType = "MultipartFile";
                    param.dataType = "MultipartFile";
                }
            }
            for (CodegenParameter param : op.formParams) {
                if (param.baseType != null && param.baseType.equalsIgnoreCase("List<int>") && isMultipart) {
                    param.baseType = "MultipartFile";
                    param.dataType = "MultipartFile";
                }
            }
            for (CodegenParameter param : op.bodyParams) {
                if (param.baseType != null && param.baseType.equalsIgnoreCase("List<int>") && isMultipart) {
                    param.baseType = "MultipartFile";
                    param.dataType = "MultipartFile";
                }
            }

            op.vendorExtensions.put("isJson", isJson);
            op.vendorExtensions.put("isProto", isProto);
            op.vendorExtensions.put("isForm", isForm);
            op.vendorExtensions.put("isMultipart", isMultipart);

            Set<String> imports = new HashSet<>();
            for (String item : op.imports) {
                if (!modelToIgnore.contains(item.toLowerCase(Locale.ROOT))) {
                    imports.add(underscore(item));
                }
            }
            modelImports.addAll(imports);
            op.imports = imports;

            String[] items = op.path.split("/", -1);
            String jaguarPath = "";

            for (int i = 0; i < items.length; ++i) {
                if (items[i].matches("^\\{(.*)\\}$")) { // wrap in {}
                    jaguarPath = jaguarPath + ":" + items[i].replace("{", "").replace("}", "");
                } else {
                    jaguarPath = jaguarPath + items[i];
                }

                if (i != items.length - 1) {
                    jaguarPath = jaguarPath + "/";
                }
            }

            op.path = jaguarPath;
        }

        objs.put("modelImports", modelImports);
        objs.put("fullImports", fullImports);

        return objs;
    }

    public Map<String, Object> dartJaguarPostProcessModels(Map<String, Object> objs) {
        objs = super.postProcessModels(objs);
        List<Object> models = (List<Object>) objs.get("models");
        ProcessUtils.addIndexToProperties(models, 1);
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            Set<String> modelImports = new HashSet<>();
            CodegenModel cm = (CodegenModel) mo.get("model");
            for (String modelImport : cm.imports) {
                if (!modelToIgnore.contains(modelImport.toLowerCase(Locale.ROOT))) {
                    modelImports.add(underscore(modelImport));
                }
            }

            for (CodegenProperty p : cm.vars) {
                String protoType = protoTypeMapping.get(p.openApiType);
                if (p.isListContainer) {
                    String innerType = protoTypeMapping.get(p.mostInnerItems.openApiType);
                    protoType = protoType + " " + (innerType == null ? p.mostInnerItems.openApiType : innerType);
                }
                p.vendorExtensions.put("x-proto-type", protoType == null ? p.openApiType : protoType);
            }

            cm.imports = modelImports;
            cm.vendorExtensions.put("hasVars", cm.vars.size() > 0);
        }
        return objs;
    }

    public Map<String, Object> dartDioPostProcessModels(Map<String, Object> objs) {
        objs = super.postProcessModels(objs);
        List<Object> models = (List<Object>) objs.get("models");
        ProcessUtils.addIndexToProperties(models, 1);
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            Set<String> modelImports = new HashSet<>();
            CodegenModel cm = (CodegenModel) mo.get("model");
            for (String modelImport : cm.imports) {
                if (importMapping.containsKey(modelImport)) {
                    modelImports.add(importMapping.get(modelImport));
                } else {
                    if (!modelToIgnore.contains(modelImport.toLowerCase(Locale.ROOT))) {
                        modelImports.add("package:" + pubName + "/model/" + underscore(modelImport) + ".dart");
                    }
                }
            }

            cm.imports = modelImports;
            cm.vendorExtensions.put("hasVars", cm.vars.size() > 0);
        }
        return objs;
    }

    public void dartDioPostProcessModelProperty(CodegenModel model, CodegenProperty property) {
        if (nullableFields) {
            property.isNullable = true;
        }

        if (property.isListContainer) {
            //Updates any List properties on a model to a BuiltList. This happens in post processing rather
            //than type mapping as we only want this to apply to models, not every other class.
            if ("List".equals(property.baseType)) {
                property.setDatatype(property.dataType.replaceAll(property.baseType, "BuiltList"));
                property.setBaseType("BuiltList");
                model.imports.add("BuiltList");
            }
        }
        if (property.isMapContainer) {
            //Updates any List properties on a model to a BuiltList. This happens in post processing rather
            //than type mapping as we only want this to apply to models, not every other class.
            if ("Map".equals(property.baseType)) {
                property.setDatatype(property.dataType.replaceAll(property.baseType, "BuiltMap"));
                property.setBaseType("BuiltMap");
                model.imports.add("BuiltMap");
            }
        }

    }
}
