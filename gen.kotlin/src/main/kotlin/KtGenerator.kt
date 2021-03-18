package kotlinx.ast.example

import kotlinx.ast.common.AstSource
import kotlinx.ast.common.ast.Ast
import kotlinx.ast.common.print
import kotlinx.ast.grammar.kotlin.common.summary
import kotlinx.ast.grammar.kotlin.target.antlr.kotlin.KotlinGrammarAntlrKotlinParser

fun main() {
    val source = AstSource.File(
            "gen.kotlin/src/main/resources/2.kt.txt"
    )
    val kotlinFile = KotlinGrammarAntlrKotlinParser.parseKotlinFile(source)
    kotlinFile.summary(attachRawAst = false)
            .onSuccess { astList ->
                astList.forEach(Ast::print)
            }.onFailure { errors ->
                errors.forEach(::println)
            }
}