package me.williamhester.kdash.web.query

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class QueryTest {
  @Test
  fun parse_parsesVariable() {
    val expression = Query.parseInternal("TelemetryVariable")

    assertThat(expression).isEqualTo(VariableExpression("TelemetryVariable"))
  }

  @Test
  fun parse_parsesFunction() {
    val expression = Query.parseInternal("LAP_DELTA(TelemetryVariable)")

    assertThat(expression).isEqualTo(FunctionExpression("LAP_DELTA", listOf(VariableExpression("TelemetryVariable"))))
  }

  @Test
  fun parse_parsesFunctionWithMultipleArguments() {
    val expression = Query.parseInternal("LAP_AVERAGE(TelemetryVariable, 5)")

    assertThat(expression)
      .isEqualTo(
        FunctionExpression("LAP_AVERAGE", listOf(VariableExpression("TelemetryVariable"), NumberExpression(5)))
      )
  }

  @Test
  fun parse_parsesSubtractionExpression() {
    val expression = Query.parseInternal("A - B")

    assertThat(expression).isEqualTo(SubtractionExpression(listOf(VariableExpression("A"), VariableExpression("B"))))
  }

  @Test
  fun parse_parsesComplexSubtractionExpression() {
    val expression = Query.parseInternal("A - LAP_AVERAGE(B, 1)")

    assertThat(expression).isEqualTo(
      SubtractionExpression(
        listOf(
          VariableExpression("A"),
          FunctionExpression("LAP_AVERAGE", listOf(VariableExpression("B"), NumberExpression(1))),
        )
      )
    )
  }
}
