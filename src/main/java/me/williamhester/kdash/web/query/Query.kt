package me.williamhester.kdash.web.query

object Query {
  private val LEGAL_CHARS = (('a'..'z') + ('A'..'Z') + '_')
  private val NUMBERS = (('0'..'9'))

  fun parse(query: String): Processor {
    val expression = parseInternal(query)
    return toProcessor(expression)
  }

  private fun toProcessor(expression: Expression): Processor {
    return when (expression) {
      is VariableExpression -> VariableProcessor(expression.value)
      is FunctionExpression -> when (expression.function) {
        "LAP_DELTA" -> LapDeltaProcessor(toProcessor(expression.params[0]))
        else -> TODO("Unimplemented function: ${expression.function}")
      }
      is NumberExpression -> TODO("Implement NumberProcessor")
    }
  }

  internal fun parseInternal(query: String): Expression {
    val trimmed = query.trim()

    if (trimmed.isEmpty()) throw QueryParseException("Expression is empty")
    if (trimmed.firstOrNull() in NUMBERS) return toNumberExpression(trimmed)

    for (i in trimmed.indices) {
      when (val char = trimmed[i]) {
        '(' -> return toFunctionExpression(trimmed.substring(0, i), findParenContents(trimmed.substring(i + 1)))
        ' ' -> return VariableExpression(trimmed.substring(0, i))
        else -> validateLegalCharacter(char)
      }
    }
    return VariableExpression(trimmed)
  }

  private fun findParenContents(afterParen: String): String {
    val trimmed = afterParen.trim()
    var parenGroups = 1

    for (i in trimmed.indices) {
      val char = trimmed[i]

      when (char) {
        '(' -> {
          parenGroups++
        }
        ')' -> {
          parenGroups--
        }
      }
      if (parenGroups < 0) throw QueryParseException("Extra right paren")
      if (parenGroups == 0) return trimmed.substring(0, i)
    }
    throw QueryParseException("Unmatched right parentheses")
  }

  private fun toFunctionExpression(functionName: String, parenContents: String): FunctionExpression {
    val args = parenContents.split(',')
    return when (functionName) {
      "LAP_DELTA" -> {
        validateArguments(functionName, 1, args.size)
        val expression = parseInternal(args[0])
        FunctionExpression(functionName, listOf(expression))
      }
      "LAP_AVERAGE" -> {
        validateArguments(functionName, 2, args.size)
        val expression1 = parseInternal(args[0])
        val expression2 = parseInternal(args[1])
        FunctionExpression(functionName, listOf(expression1, expression2))
      }
      else -> throw QueryParseException("Unknown function: '$functionName'")
    }
  }

  private fun toNumberExpression(value: String): NumberExpression {
    try {
      return NumberExpression(value.toInt())
    } catch (e: NumberFormatException) {
      throw QueryParseException("Expression started with a number but was not a number: '$value'")
    }
  }

  private fun validateArguments(functionName: String, expectedArgs: Int, actualArgs: Int) {
    if (actualArgs != expectedArgs) throw QueryParseException("$functionName expected 1 argument. Received $actualArgs")
  }

  private fun validateLegalCharacter(char: Char) {
    if (char !in LEGAL_CHARS) throw QueryParseException("Illegal character: '$char'")
  }
}

internal sealed interface Expression

internal data class VariableExpression(val value: String) : Expression

internal data class FunctionExpression(val function: String, val params: List<Expression>) : Expression

internal data class NumberExpression(val value: Int) : Expression

class QueryParseException(val error: String) : Exception(error)
