package adapter
import io.outfoxx.swiftpoet.TupleTypeName

internal fun TupleTypeName.generateTuple(paramName: String): String =
    if (this.types.size == 2) {
        "$paramName: ("
            .plus(
                this.types[0].second
                    .generateSwiftRetrieverForKotlinType(
                        "obj.$paramName.first",
                    ),
            )
            .plus(", ")
            .plus(
                this.types[1].second
                    .generateSwiftRetrieverForKotlinType(
                        "obj.$paramName.second",
                    ),
            )
            .plus(")")
    } else {
        "$paramName: ("
            .plus(
                this.types[0].second
                    .generateSwiftRetrieverForKotlinType(
                        "obj.$paramName.first",
                    ),
            )
            .plus(", ")
            .plus(
                this.types[1].second
                    .generateSwiftRetrieverForKotlinType(
                        "obj.$paramName.second",
                    ),
            )
            .plus(", ")
            .plus(
                this.types[2].second
                    .generateSwiftRetrieverForKotlinType(
                        "obj.$paramName.third",
                    ),
            )
            .plus(")")
    }


internal fun ParameterizedTypeName.toArrayCaster(
    paramName: String,
    optional: Boolean = false,
): String = "$paramName: "
    .plus(
        if (optional) "obj.$paramName != nil ? " else "",
    )
    .plus("obj.$paramName as! ")
    .plus(this.unwrapOptional().kotlinInteropTypeWithFallback)
    .plus(if (optional) " : nil" else "")

internal fun ParameterizedTypeName.toSetCaster(
    paramName: String,
    optional: Boolean = false,
): String = "$paramName: "
    .plus(
        if (optional) "obj.$paramName != nil ? " else "",
    )
    .plus("obj.$paramName as! Set<")
    .plus(
        (this.unwrapOptional() as ParameterizedTypeName).typeArguments[0].kotlinInteropTypeWithFallback
    )
    .plus(">")
    .plus(if (optional) " : nil" else "")

internal fun ParameterizedTypeName.toDictionaryCaster(
    paramName: String,
    optional: Boolean = false,
): String = "$paramName: "
    .plus(
        if (optional) "obj.$paramName != nil ? " else "",
    )
    .plus("obj.$paramName as! [")
    .plus(this.typeArguments[0].kotlinInteropTypeWithFallback)
    .plus(" : ")
    .plus(this.typeArguments[1].kotlinInteropTypeWithFallback)
    .plus("]")
    .plus(if (optional) " : nil" else "")

internal fun TypeName.generateInitParameter(paramName: String): String {
    return "$paramName: "
        .plus(
            this.generateSwiftRetrieverForKotlinType(
                paramName = "obj.$paramName",
                isForTuple = false,
            ),
        )
}