package adapter

import kotlin.reflect.KClass

class KtAdapterFeature(
    override val featureContext: KClass<ClassContext>,
    override val filter: Filter<ClassContext>,
) : ProcessorFeature<ClassContext>() {

    @Suppress("ReturnCount")
    override fun doProcess(featureContext: ClassContext, processorContext: ProcessorContext) {
        if (featureContext.clazz.sealedSubclasses.isEmpty()) return
        val kotlinFrameworkName: String = processorContext.framework.baseName
        val kmClass: KmClass = featureContext.clazz
        if (Flag.IS_PUBLIC(kmClass.flags).not()) return
        val sealedCases: List<EnumCase> = buildEnumCases(kotlinFrameworkName, featureContext)
        if (sealedCases.isEmpty()) return
        val typeVariables: List<TypeVariableName> =
            kmClass.buildTypeVariableNames(kotlinFrameworkName)
        val originalClassName: String = getSimpleName(kmClass.name, featureContext.kLibClasses)
        val className: String = originalClassName.replace(".", "").plus("Ks")
        val enumType: TypeSpec = TypeSpec.enumBuilder(className)
            .addDoc("selector: ${featureContext.prefixedUniqueId}")
            .apply {
                typeVariables.forEach { addTypeVariable(it) }
                sealedCases.forEach { addEnumCase(it.enumCaseSpec) }
            }
            .addModifiers(Modifier.PUBLIC)
            .addFunction(
                buildEnumConstructor(
                    featureContext = featureContext,
                    kotlinFrameworkName = kotlinFrameworkName,
                    sealedCases = sealedCases,
                    className = className,
                    originalClassName = originalClassName,
                ),
            )
            .addProperty(
                buildSealedProperty(
                    featureContext = featureContext,
                    kotlinFrameworkName = kotlinFrameworkName,
                    sealedCases = sealedCases,
                ),
            )
            .build()

        processorContext.fileSpecBuilder.addType(enumType)
    }
    private fun buildEnumConstructor(
        featureContext: ClassContext,
        kotlinFrameworkName: String,
        sealedCases: List<EnumCase>,
        className: String,
        originalClassName: String,
    ): FunctionSpec {
        return FunctionSpec.builder("init")
            .addModifiers(Modifier.PUBLIC)
            .addParameter(
                label = "_",
                name = "obj",
                type = featureContext.clazz.getDeclaredTypeNameWithGenerics(
                    kotlinFrameworkName = kotlinFrameworkName,
                    classes = featureContext.kLibClasses,
                ),
            )
            .addCode(
                CodeBlock.builder()
                    .apply {
                        sealedCases.forEachIndexed { index, enumCase ->
                            buildString {
                                if (index != 0) append("} else ")
                                append("if ")
                                append(enumCase.initCheck)
                                append(" {")
                                append('\n')
                            }.also { add(it) }
                            indent()
                            buildString {
                                append("self = .")
                                append(enumCase.name)
                                append(enumCase.initBlock)
                                append('\n')
                            }.also { add(it) }
                            unindent()
                        }
                        add("} else {\n")
                        indent()
                        add("fatalError(\"$className not synchronized with $originalClassName class\")\n")
                        unindent()
                        add("}\n")
                    }
                    .build(),
            )
            .build()
    }

    private fun buildEnumCases(
        kotlinFrameworkName: String,
        featureContext: ClassContext,
    ): List<EnumCase> {
        val kmClass = featureContext.clazz
        return kmClass.sealedSubclasses.mapNotNull { sealedClassName ->
            val sealedClass: KmClass = featureContext.parentContext
                .fragment.classes.first { it.name == sealedClassName }
            if (Flag.IS_PUBLIC(sealedClass.flags).not()) return@mapNotNull null
            buildEnumCase(kotlinFrameworkName, featureContext, sealedClassName, sealedClass)
        }
    }
    private fun buildEnumCase(
        kotlinFrameworkName: String,
        featureContext: ClassContext,
        subclassName: ClassName,
        sealedCaseClass: KmClass,
    ): EnumCase {
        val kmClass = featureContext.clazz
        val name: String = if (subclassName.startsWith(kmClass.name)) {
            subclassName.removePrefix(kmClass.name).removePrefix(".")
        } else {
            subclassName.removePrefix(kmClass.name.substringBeforeLast("/")).removePrefix("/")
        }
        val decapitalizedName: String = name.decapitalize(Locale.ROOT)

        val isObject: Boolean = Flag.Class.IS_OBJECT(sealedCaseClass.flags)
        val caseArg = sealedCaseClass.getDeclaredTypeNameWithGenerics(
            kotlinFrameworkName = kotlinFrameworkName,
            classes = featureContext.kLibClasses,
        )

        return EnumCase(
            name = decapitalizedName,
            param = if (isObject) null else caseArg,
            initCheck = if (isObject) {
                "obj is $caseArg"
            } else {
                "let obj = obj as? $caseArg"
            },
            initBlock = if (isObject) "" else "(obj)",
            caseArg = caseArg,
            caseBlock = if (isObject) "" else "(let obj)",
        )
    }

    private fun buildSealedProperty(
        featureContext: ClassContext,
        kotlinFrameworkName: String,
        sealedCases: List<EnumCase>,
    ): PropertySpec {
        val returnType: TypeName = featureContext.clazz.getDeclaredTypeNameWithGenerics(
            kotlinFrameworkName = kotlinFrameworkName,
            classes = featureContext.kLibClasses,
        )
        return PropertySpec.builder("sealed", type = returnType)
            .addModifiers(Modifier.PUBLIC)
            .getter(
                FunctionSpec
                    .getterBuilder()
                    .addCode(buildSealedPropertyBody(sealedCases, returnType))
                    .build(),
            ).build()
    }

    private fun buildSealedPropertyBody(
        sealedCases: List<EnumCase>,
        returnType: TypeName,
    ): CodeBlock = CodeBlock.builder().apply {
        add("switch self {\n")
        sealedCases.forEach { enumCase ->
            buildString {
                append("case .")
                append(enumCase.name)
                append(enumCase.caseBlock)
                append(":\n")
            }.also { add(it) }
            indent()
            addSealedCaseReturnCode(enumCase, returnType)
            unindent()
        }
        add("}\n")
    }.build()

    private fun CodeBlock.Builder.addSealedCaseReturnCode(
        enumCase: EnumCase,
        returnType: TypeName,
    ) {
        val paramType: TypeName? = enumCase.param
        val cast: String
        val returnedName: String
        if (paramType == null) {
            returnedName = "${enumCase.caseArg}()"
            cast = if (returnType is ParameterizedTypeName) {
                "as!"
            } else {
                "as"
            }
        } else {
            returnedName = "obj"
            cast = if (paramType is ParameterizedTypeName && returnType is ParameterizedTypeName) {
                if (paramType.typeArguments == returnType.typeArguments) {
                    "as"
                } else {
                    "as!"
                }
            } else {
                "as!"
            }
        }
        add("return $returnedName $cast $returnType\n")
    }
    data class EnumCase(
        val name: String,
        val param: TypeName?,
        val initCheck: String,
        val initBlock: String,
        val caseArg: TypeName,
        val caseBlock: String,
    ) {
        val enumCaseSpec: EnumerationCaseSpec
            get() {
                return if (param == null) {
                    EnumerationCaseSpec.builder(name)
                } else {
                    EnumerationCaseSpec.builder(name, param)
                }.build()
            }
    }
    class Config : BaseConfig<ClassContext> {
        override var filter: Filter<ClassContext> = Filter.Exclude(emptySet())
    }
    companion object : Factory<ClassContext, SealedToSwiftEnumFeature, Config> {
        override fun create(block: Config.() -> Unit): SealedToSwiftEnumFeature {
            val config = Config().apply(block)
            return SealedToSwiftEnumFeature(featureContext, config.filter)
        }
        override val featureContext: KClass<ClassContext> = ClassContext::class

        @JvmStatic
        override val factory = Companion
    }
}