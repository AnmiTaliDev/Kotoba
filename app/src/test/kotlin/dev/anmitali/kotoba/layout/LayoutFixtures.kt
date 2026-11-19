package dev.anmitali.kotoba.layout

import org.json.JSONObject

object LayoutFixtures {

    const val Minimal = """
    {
      "id": "test",
      "name": "Test",
      "locale": "en",
      "defaultLayer": "letters",
      "punctuation": {"sentenceEnd": ".", "doubleSpace": "."},
      "layers": {
        "letters": {
          "shiftable": true,
          "rows": [
            ["a", "b"],
            [{"type": "shift"}, {"type": "space", "weight": 4}, {"type": "delete"}]
          ]
        }
      }
    }
    """

    fun minimal(): JSONObject = JSONObject(Minimal)

    fun parse(json: JSONObject): LayoutParseResult = LayoutParser.parse(json)

    fun errors(result: LayoutParseResult): List<LayoutIssue> =
        result.issues.filter { it.severity == Severity.Error }

    fun warnings(result: LayoutParseResult): List<LayoutIssue> =
        result.issues.filter { it.severity == Severity.Warning }
}
