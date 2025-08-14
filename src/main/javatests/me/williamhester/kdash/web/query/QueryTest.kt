package me.williamhester.kdash.web.query

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class QueryTest {
  @Test
  fun parseToExpression_parsesVariable() {
    val expression = Query.parseToExpression("TelemetryVariable").tokens

    assertThat(expression).containsExactly(VariableToken("TelemetryVariable")).inOrder()
  }

  @Test
  fun parseToExpression_parsesFunction() {
    val expression = Query.parseToExpression("LAP_DELTA(TelemetryVariable)").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken("LAP_DELTA", listOf(Expression(listOf(VariableToken("TelemetryVariable")))))
      )
      .inOrder()
  }

  @Test
  fun parseToExpression_parsesFunctionWithMultipleArguments() {
    val expression = Query.parseToExpression("LAP_AVERAGE(TelemetryVariable, 5)").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken("LAP_AVERAGE", listOf(Expression(listOf(VariableToken("TelemetryVariable"))), Expression(listOf(NumberToken(5)))))
      )
      .inOrder()
  }

  @Test
  fun parseToExpression_parsesSubtractionExpression() {
    val expression = Query.parseToExpression("A - B").tokens

    assertThat(expression)
      .containsExactly(
        VariableToken("A"), OperatorToken.SUBTRACT, VariableToken("B")
      )
      .inOrder()
  }

  @Test
  fun parseToExpression_parsesComplexSubtractionExpression() {
    val expression = Query.parseToExpression("A - LAP_AVERAGE(B, 1)").tokens

//    assertThat(expression).isEqualTo(
//      SubtractionExpression(
//        listOf(
//          VariableToken("A"),
//          FunctionToken("LAP_AVERAGE", listOf(VariableToken("B"), NumberExpression(1))),
//        )
//      )
//    )
  }

  @Test
  fun parseToExpression_whatever() {
    val expression = Query.parseToExpression("Function(1, Function2(Param2))").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken(
          "Function",
          listOf(
            Expression(listOf(NumberToken(1))),
            Expression(listOf(FunctionToken("Function2", listOf(Expression(listOf(VariableToken("Param2"))))))),
          ),
        )
      ).inOrder()
  }

  @Test
  fun parseToExpression_whatever2() {
    val expression = Query.parseToExpression("1 - 2").tokens

    assertThat(expression)
      .containsExactly(
        NumberToken(1),
        OperatorToken.SUBTRACT,
        NumberToken(2),
      ).inOrder()
  }

  @Test
  fun parseToExpression_whatever3() {
    val expression = Query.parseToExpression("Function1(1 - 2, 3)").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken(
          "Function1",
          listOf(
            Expression(
              listOf(
                NumberToken(1),
                OperatorToken.SUBTRACT,
                NumberToken(2),
              )
            ),
            Expression(listOf(NumberToken(3))),
          ),
        )
      ).inOrder()
  }

  @Test
  fun parseToExpression_parsesOperators() {
    val expression = Query.parseToExpression("1 + 2 - 3 * 4 / 5").tokens

    assertThat(expression)
      .containsExactly(
        NumberToken(1),
        OperatorToken.ADD,
        NumberToken(2),
        OperatorToken.SUBTRACT,
        NumberToken(3),
        OperatorToken.MULTIPLY,
        NumberToken(4),
        OperatorToken.DIVIDE,
        NumberToken(5),
      ).inOrder()
  }

  @Test
  fun parseToExpression_parsesParentheses() {
    val expression = Query.parseToExpression("1 + (2 - 3)").tokens

    assertThat(expression)
      .containsExactly(
        NumberToken(1),
        OperatorToken.ADD,
        ParentheticalExpression(
          Expression(
            listOf(
              NumberToken(2),
              OperatorToken.SUBTRACT,
              NumberToken(3),
            )
          )
        )
      ).inOrder()
  }

  @Test
  fun parseToExpression_parsesFunctions2() {
    val expression = Query.parseToExpression("LAP_AVERAGE(LAP_DELTA(VarName), 1)").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken(
          "LAP_AVERAGE",
          listOf(
            Expression(listOf(FunctionToken("LAP_DELTA", listOf(Expression(listOf(VariableToken("VarName"))))))),
            Expression(listOf(NumberToken(1))),
          )
        )
      ).inOrder()
  }

  @Test
  fun parseToExpression_parsesFunctions_noSpaces() {
    val expression = Query.parseToExpression("LAP_AVERAGE(LAP_DELTA(VarName),1)").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken(
          "LAP_AVERAGE",
          listOf(
            Expression(listOf(FunctionToken("LAP_DELTA", listOf(Expression(listOf(VariableToken("VarName"))))))),
            Expression(listOf(NumberToken(1))),
          )
        )
      ).inOrder()
  }

  @Test
  fun parseToExpression_parsesFunctionsWith3Params() {
    val expression = Query.parseToExpression("LAP_AVERAGE(LAP_DELTA(VarName), 1, 2)").tokens

    assertThat(expression)
      .containsExactly(
        FunctionToken(
          "LAP_AVERAGE",
          listOf(
            Expression(listOf(FunctionToken("LAP_DELTA", listOf(Expression(listOf(VariableToken("VarName"))))))),
            Expression(listOf(NumberToken(1))),
            Expression(listOf(NumberToken(2))),
          )
        )
      ).inOrder()
  }
}
