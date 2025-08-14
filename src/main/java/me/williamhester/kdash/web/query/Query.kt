package me.williamhester.kdash.web.query

import me.williamhester.kdash.web.models.DataPoint
import me.williamhester.kdash.web.models.TelemetryDataPoint

object Query {
  private val FUNCTION_OR_VARIABLE_NAMES = (('a'..'z') + ('A'..'Z') + '_')
  private val NUMBERS = (('0'..'9'))
  private val OPERATOR_CHARACTERS = setOf('+', '-', '*', '/')
  private val CONTROL_CHARACTERS = setOf(' ', '(', ')', ',') + OPERATOR_CHARACTERS

  fun parse(query: String): Processor {
    val expression = parseToExpression(query)
    return toProcessor(expression)
  }

  private fun toProcessor(expression: Expression): Processor {
    val tokensIterator = expression.tokens.iterator()
    if (!tokensIterator.hasNext()) throw QueryParseException("Empty query")

    val processors = mutableListOf<OperatorAndProcessor>()

    val processor = toProcessorNonOperator(tokensIterator.next())
    processors += OperatorAndProcessor(OperatorToken.ADD, processor)

    while (tokensIterator.hasNext()) {
      val operatorToken = tokensIterator.next()
      if (operatorToken !is OperatorToken) throw QueryParseException("Unexpected token: $operatorToken")

      if (!tokensIterator.hasNext()) throw QueryParseException("Unexpected end of query.")

      val nextProcessor = toProcessorNonOperator(tokensIterator.next())
      processors += OperatorAndProcessor(operatorToken, nextProcessor)
    }

    return CompositeProcessor(processors)
  }

  private class CompositeProcessor(private val operatorsAndProcessors: List<OperatorAndProcessor>) : Processor {
    override val requiredOffset: Float = operatorsAndProcessors.maxOf { it.processor.requiredOffset }

    override fun process(telemetryDataPoint: TelemetryDataPoint): DataPoint {
      var result = 0.0
      for ((operator, processor) in operatorsAndProcessors) {
        val processorResult = processor.process(telemetryDataPoint)

        when (operator) {
          OperatorToken.ADD -> result += processorResult.value
          OperatorToken.SUBTRACT -> result -= processorResult.value
          OperatorToken.MULTIPLY -> result *= processorResult.value
          OperatorToken.DIVIDE -> result /= processorResult.value
        }
      }
      return DataPoint(telemetryDataPoint.sessionTime, telemetryDataPoint.driverDistance, result)
    }
  }

  private fun toProcessorNonOperator(token: Token) = when (token) {
    is NumberToken -> NumberProcessor(token.value)
    is VariableToken -> VariableProcessor(token.value)
    is ParentheticalExpression -> toProcessor(token.expression)
    is FunctionToken -> when (token.value) {
      "LAP_DELTA" -> {
        validateArguments("LAP_DELTA", 1, token.tokens.size)
        LapDeltaProcessor(toProcessor(token.tokens.first()))
      }
      "LAP_AVERAGE" -> {
        validateArguments("LAP_AVERAGE", 2, token.tokens.size)
        val arg1 = token.tokens[0]
        val arg2 = token.tokens[1].tokens[0]
        if (arg2 !is NumberToken) throw QueryParseException("LAP_AVERAGE expects an integer for the second parameter.")
        LapAverageProcessor(toProcessor(arg1), arg2.value)
      }
      else -> throw QueryParseException("Unknown function: ${token.value}")
    }
    is OperatorToken -> throw QueryParseException("Unexpected operator: $token")
  }

  private data class OperatorAndProcessor(val operatorToken: OperatorToken, val processor: Processor)

  internal fun parseToExpression(query: String): Expression = Expression(parseInternal(query).first)

  internal fun parseInternal(query: String, terminatingChars: Set<Char> = setOf()): Pair<List<Token>, Int> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) {
      throw QueryParseException("Empty query")
    }

    val tokens = mutableListOf<Token>()
    var i = 0
    var startOfExpression = 0
    while (i < trimmed.length) {
      val (token, charsToSkip) =
        when (val char = trimmed[i]) {
          in NUMBERS -> parseNumber(trimmed.substring(startOfExpression))
          in FUNCTION_OR_VARIABLE_NAMES -> {
            parseFunctionOrVariable(trimmed.substring(startOfExpression), terminatingChars)
          }
          in OPERATOR_CHARACTERS -> parseOperator(char)
          ' ' -> {
            i++
            startOfExpression = i
            continue
          }
          '(' -> parseParentheticalExpression(trimmed.substring(startOfExpression))
          in terminatingChars -> {
            i++
            break
          }
          else -> throw QueryParseException("Unexpected start of query: '$char'")
        }
      tokens += token
      i += charsToSkip
    }
    return tokens to i + (query.length - trimmed.length)
  }

  internal fun parseNumber(query: String): Pair<Token, Int> {
    var i = 1
    while (i < query.length) {
      when (val char = query[i]) {
        in NUMBERS -> {} // Do nothing
        in CONTROL_CHARACTERS -> break
        else -> throw QueryParseException("Unexpected character while parsing number. Found '$char'.")
      }
      i++
    }
    return NumberToken(query.substring(0, i).toInt()) to i
  }

  internal fun parseFunctionOrVariable(query: String, terminatingChars: Set<Char> = setOf()): Pair<Token, Int> {
    var i = 1
    while (i < query.length) {
      when (val char = query[i]) {
        in FUNCTION_OR_VARIABLE_NAMES,
        in NUMBERS -> {} // Do nothing
        ' ' -> {
          break
        }
        in terminatingChars -> {
          break
        }
        '(' -> return parseFunction(query.substring(0, i), query.substring(i))
        else -> throw QueryParseException("Unexpected character while parsing variable or function. Found '$char'.")
      }
      i++
    }
    return VariableToken(query.substring(0, i)) to i
  }

  internal fun parseOperator(char: Char): Pair<Token, Int> {
    return when (char) {
      '+' -> OperatorToken.ADD
      '-' -> OperatorToken.SUBTRACT
      '*' -> OperatorToken.MULTIPLY
      '/' -> OperatorToken.DIVIDE
      else -> throw AssertionError("Unsupported control character '$char'")
    } to 1
  }

  internal fun parseParentheticalExpression(query: String): Pair<Token, Int> {
    val contents = findParenContents(query.substring(1))
    val (tokens, charsToSkip) = parseInternal(contents)
    // +2 to account for the parentheses.
    return ParentheticalExpression(Expression(tokens)) to charsToSkip + 2
  }

  internal fun parseFunction(functionName: String, remainingQuery: String): Pair<Token, Int> {
    val afterParen = findParenContents(remainingQuery.substring(1))
    val trimmed = afterParen.trim()

    val expressions = mutableListOf<Expression>()
    var i = 0
    var startOfExpression = 0
    while (i < trimmed.length) {
      val (tokens, charsToSkip) = parseInternal(trimmed.substring(startOfExpression), setOf(','))
      expressions += Expression(tokens)
      i += charsToSkip
      startOfExpression += charsToSkip
    }
    // Function name + params length + params surrounding whitespace length + 2 (one for each parenthesis)
    val charsToSkip = functionName.length + i + (afterParen.length - trimmed.length) + 2
    return FunctionToken(functionName, expressions) to charsToSkip
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

  private fun validateArguments(functionName: String, expectedArgs: Int, actualArgs: Int) {
    if (actualArgs != expectedArgs) throw QueryParseException("$functionName expected $expectedArgs argument. Received $actualArgs")
  }
}

internal sealed interface Token

internal data class NumberToken(val value: Int) : Token

internal data class VariableToken(val value: String) : Token

internal data class FunctionToken(val value: String, val tokens: List<Expression>) : Token

internal data class ParentheticalExpression(val expression: Expression) : Token

internal enum class OperatorToken : Token {
  ADD,
  SUBTRACT,
  MULTIPLY,
  DIVIDE,
}

internal data class Expression(val tokens: List<Token>)

class QueryParseException(val error: String) : Exception(error)
