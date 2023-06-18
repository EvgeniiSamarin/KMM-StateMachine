package adapter

import kotlin.reflect.KClass

class SealetToSwiftEnum(
    override val featureContext: KClass<ClassContext>,
    override val filter: Filter<ClassContext>,
) : ProcessorFeature<ClassContext>() {

    @Suppress("ReturnCount")
    override fun doProcess(featureContext: ClassContext, processorContext: ProcessorContext) {
        val kotlinFrameworkName: String = processorContext.framework.baseName

        doProcess(
            featureContext = featureContext,
            fileSpecBuilder = processorContext.fileSpecBuilder,
            kotlinFrameworkName = kotlinFrameworkName,
        )
    }

    fun doProcess(
        featureContext: ClassContext,
        fileSpecBuilder: FileSpec.Builder,
        kotlinFrameworkName: String,
    ) {
        val kmClass: KmClass = featureContext.clazz
        val originalClassName: String = getSimpleName(kmClass.name, featureContext.kLibClasses)

        if (!Flag.IS_PUBLIC(kmClass.flags) || featureContext.clazz.sealedSubclasses.isEmpty()) {
            return
        }

        val sealedCases: List<AssociatedEnumCase> = buildEnumCases(
            kotlinFrameworkName = kotlinFrameworkName,
            featureContext = featureContext,
        )
        if (sealedCases.isEmpty()) {
            logger.warn("No public subclasses found for sealed class $originalClassName")
            return
        } else {
            logger.lifecycle(
                "Generating enum for sealed class $originalClassName (${sealedCases.size} public subclasses)",
            )
        }

        val enumType: TypeSpec = buildTypeSpec(
            featureContext = featureContext,
            typeVariables = kmClass.buildTypeVariableNames(kotlinFrameworkName),
            sealedCases = sealedCases,
            kotlinFrameworkName = kotlinFrameworkName,
            originalClassName = originalClassName,
        )

        fileSpecBuilder.addType(enumType)
    }

    class Config : BaseConfig<ClassContext> {
        override var filter: Filter<ClassContext> = Filter.Exclude(emptySet())
    }

    companion object : Factory<ClassContext, SealedToSwiftAssociatedEnumFeature, Config> {
        override fun create(block: Config.() -> Unit): SealedToSwiftAssociatedEnumFeature {
            val config = Config().apply(block)
            return SealedToSwiftAssociatedEnumFeature(featureContext, config.filter)
        }

        override val featureContext: KClass<ClassContext> = ClassContext::class

        @JvmStatic
        override val factory = Companion

        val logger: Logger = Logging.getLogger("SealedToSwiftAssociatedEnumFeature")
    }
}