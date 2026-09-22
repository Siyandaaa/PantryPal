package com.motivation.pantrypal.data.remote

import com.motivation.pantrypal.data.remote.dto.SpoonacularAutocompleteItem
import com.motivation.pantrypal.data.remote.dto.SpoonacularFindByIngredientsItem
import com.motivation.pantrypal.data.remote.dto.SpoonacularParsedIngredient
import com.motivation.pantrypal.data.remote.dto.SpoonacularRecipeInformation
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Called directly by the app - recipe discovery isn't part of PantryPal's
 * one required custom API (that's authentication only; see root README,
 * "API Scope"). The API key is attached to every call automatically by
 * [SpoonacularKeyInterceptor] rather than passed as a parameter here.
 */
interface SpoonacularApiService {

    /** "What's in My Pantry?": ranks recipes by how many of [ingredients] they use. */
    @GET("recipes/findByIngredients")
    suspend fun findByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 20,
        @Query("ranking") ranking: Int = 2,
        @Query("ignorePantry") ignorePantry: Boolean = true
    ): List<SpoonacularFindByIngredientsItem>

    @GET("recipes/{id}/information")
    suspend fun getRecipeInformation(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = true
    ): SpoonacularRecipeInformation

    @GET("recipes/informationBulk")
    suspend fun getRecipeInformationBulk(@Query("ids") commaSeparatedIds: String): List<SpoonacularRecipeInformation>

    @GET("food/ingredients/autocomplete")
    suspend fun autocompleteIngredient(
        @Query("query") query: String,
        @Query("number") number: Int = 8
    ): List<SpoonacularAutocompleteItem>

    /** Backs Voice-Activated Pantry Entry: turns a raw speech transcript into structured ingredients. */
    @FormUrlEncoded
    @POST("recipes/parseIngredients")
    suspend fun parseIngredients(
        @Field("ingredientList") ingredientList: String,
        @Field("servings") servings: Int = 1
    ): List<SpoonacularParsedIngredient>
}
