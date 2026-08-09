package com.dietcoach.app

import com.dietcoach.app.ai.AiParsers
import com.dietcoach.app.domain.MealType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiParsersTest {

    @Test
    fun parseFood_fromJsonObject() {
        val raw = """
            {"items":[{"name":"鸡胸肉","amount":"150g","kcal":165,"protein_g":31,"carb_g":0,"fat_g":3.6,"confidence":0.8}],"notes":"ok","meal_hint":"LUNCH"}
        """.trimIndent()
        val result = AiParsers.parseFood(raw)
        assertEquals(1, result.items.size)
        assertEquals("鸡胸肉", result.items[0].name)
        assertEquals(165, result.items[0].kcal)
        assertEquals(MealType.LUNCH, AiParsers.mealHintToType(result.mealHint, MealType.SNACK))
    }

    @Test
    fun parseFood_stripsMarkdownFence() {
        val raw = """
            ```json
            {"items":[{"name":"米饭","amount":"1碗","kcal":200,"protein_g":4,"carb_g":44,"fat_g":0.5,"confidence":0.7}],"notes":"x"}
            ```
        """.trimIndent()
        val result = AiParsers.parseFood(raw)
        assertEquals("米饭", result.items.first().name)
        assertTrue(result.items.first().carbG > 0)
    }

    @Test
    fun parseWeekPlan_readsSummary() {
        val raw = """{"title":"计划","daily_calorie_range":[1600,1900],"protein_target_g":130,"training_days_per_week":4,"focus":["蛋白"],"days":[],"warnings":[],"summary_markdown":"## 你好"}"""
        val plan = AiParsers.parseWeekPlan(raw)
        assertEquals("计划", plan.title)
        assertEquals("## 你好", AiParsers.toPrettyMarkdown(plan))
    }

    @Test
    fun extractTaggedJson_ignoresSpacesAndCase() {
        val raw = """
            消耗大约 220 kcal。
            << strength_json >>{"category":"LEGS","exerciseName":"深蹲","sets":4,"reps":8,"loadKg":100,"minutes":40,"kcal":220}
            <<WORKOUT_JSON>>{"name":"慢跑","minutes":20,"intensity":"LOW","kcal":120}
        """.trimIndent()
        val json = AiParsers.extractTaggedJson(raw, "STRENGTH_JSON")
        assertTrue(json != null && json.contains("深蹲"))
        assertTrue(!json!!.contains("慢跑"))
        assertTrue(AiParsers.wantsRecord("今天深蹲帮我记录一下"))
        assertEquals(2, AiParsers.extractAllTaggedPairs(raw).size)
    }

    @Test
    fun parseFoodTagPayload_keepsMultipleItems() {
        val json = """
            {"meal_hint":"LUNCH","items":[
              {"name":"麦当劳汉堡","amount":"1个","kcal":520,"protein_g":25,"carb_g":45,"fat_g":28},
              {"name":"鸡翅","amount":"2块","kcal":180,"protein_g":12,"carb_g":8,"fat_g":12}
            ]}
        """.trimIndent()
        val items = AiParsers.parseFoodTagPayload(json)
        assertEquals(2, items.size)
        assertEquals("麦当劳汉堡", items[0].name)
        assertEquals("鸡翅", items[1].name)
        assertEquals("LUNCH", items[0].mealHint)

        val multiTag = """
            <<FOOD_JSON>>{"name":"汉堡","amount":"1个","kcal":500,"protein_g":20,"carb_g":40,"fat_g":25,"meal_hint":"LUNCH"}
            <<FOOD_JSON>>{"name":"鸡翅","amount":"2块","kcal":180,"protein_g":12,"carb_g":8,"fat_g":12,"meal_hint":"LUNCH"}
        """.trimIndent()
        assertEquals(2, AiParsers.extractAllTaggedPairs(multiTag).count { it.first == "FOOD_JSON" })
    }
}
