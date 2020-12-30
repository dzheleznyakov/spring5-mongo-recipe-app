package guru.springframework.controllers;

import guru.springframework.commands.IngredientCommand;
import guru.springframework.commands.UnitOfMeasureCommand;
import guru.springframework.converters.IngredientCommandToIngredient;
import guru.springframework.converters.RecipeToRecipeCommand;
import guru.springframework.domain.Ingredient;
import guru.springframework.domain.Recipe;
import guru.springframework.services.IngredientService;
import guru.springframework.services.RecipeService;
import guru.springframework.services.UnitOfMeasureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by jt on 6/28/17.
 */
@Slf4j
@Controller
public class IngredientController {
    private static final String INGREDIENTFORM_URL = "recipe/ingredient/ingredientform";
    private static final String INGREDIENTS_SHOW = "recipe/ingredient/show";

    private final IngredientService ingredientService;
    private final RecipeService recipeService;
    private final UnitOfMeasureService unitOfMeasureService;
    private final IngredientCommandToIngredient ingredientCommandToIngredient;
    private final RecipeToRecipeCommand recipeToRecipeCommand;

    public IngredientController(
            IngredientService ingredientService,
            RecipeService recipeService,
            UnitOfMeasureService unitOfMeasureService,
            IngredientCommandToIngredient ingredientCommandToIngredient,
            RecipeToRecipeCommand recipeToRecipeCommand) {
        this.ingredientService = ingredientService;
        this.recipeService = recipeService;
        this.unitOfMeasureService = unitOfMeasureService;
        this.ingredientCommandToIngredient = ingredientCommandToIngredient;
        this.recipeToRecipeCommand = recipeToRecipeCommand;
    }

    @GetMapping("/recipe/{recipeId}/ingredients")
    public String listIngredients(@PathVariable String recipeId, Model model) {
        log.debug("Getting ingredient list for recipe id: " + recipeId);

        // use command object to avoid lazy load errors in Thymeleaf.
        model.addAttribute("recipe", recipeService.findCommandById(recipeId));

        return "recipe/ingredient/list";
    }

    @GetMapping("recipe/{recipeId}/ingredient/{id}/show")
    public String showRecipeIngredient(@PathVariable String recipeId,
                                       @PathVariable String id,
                                       Model model
    ) {
        model.addAttribute("ingredient", ingredientService.findByRecipeIdAndIngredientId(recipeId, id));
        return INGREDIENTS_SHOW;
    }

    @GetMapping("recipe/{recipeId}/ingredient/new")
    public String newRecipe(@PathVariable String recipeId, Model model) {
        //todo raise exception if null

        //need to return back parent id for hidden form property
        IngredientCommand ingredientCommand = new IngredientCommand();
        ingredientCommand.setRecipeId(recipeId);
        model.addAttribute("ingredient", ingredientCommand);

        //init uom
        ingredientCommand.setUom(new UnitOfMeasureCommand());

        return "recipe/ingredient/ingredientform";
    }

    @GetMapping("recipe/{recipeId}/ingredient/{id}/update")
    public String updateRecipeIngredient(@PathVariable String recipeId,
                                         @PathVariable String id,
                                         Model model) {
        model.addAttribute("ingredient", ingredientService.findByRecipeIdAndIngredientId(recipeId, id));

        return INGREDIENTFORM_URL;
    }

    @PostMapping("recipe/{recipeId}/ingredient")
    public Mono<String> saveOrUpdate(
            @Valid @ModelAttribute("ingredient") Mono<IngredientCommand> command,
            @PathVariable("recipeId") String recipeId,
            Model model
    ) {
        AtomicReference<String> ingredientId = new AtomicReference<>();
        return command
                .map(ingredientCommand -> {
                    ingredientCommand.setRecipeId(recipeId);
                    ingredientId.set(ingredientCommand.getId());
                    return ingredientCommand;
                })
                .zipWith(recipeService.findById(recipeId))
                .flatMap(zipped -> {
                    IngredientCommand ingredientCommand = zipped.getT1();
                    Recipe recipe = zipped.getT2();
                    return recipe.getIngredients()
                            .stream()
                            .filter(ingredient -> Objects.equals(ingredient.getId(), ingredientCommand.getId()))
                            .findFirst()
                            .map(ingredient -> {
                                updateIngredient(ingredientCommand, ingredient);
                                return Mono.just(recipe);
                            })
                            .orElseGet(() -> {
                                Ingredient newIngredient = ingredientCommandToIngredient.convert(ingredientCommand);
                                return addNewIngredient(ingredientCommand, recipe, newIngredient);
                            });
                })
//                .map(recipeToRecipeCommand::convert)
                .flatMap(recipeService::save)
                .map(savedCommand -> {
                    log.debug("saved recipe id: " + savedCommand.getId());
                    return "redirect:/recipe/" + recipeId + "/ingredients";
                })
                .doOnError(thr -> log.error("Error saving ingredient: " + thr))
                .onErrorResume(WebExchangeBindException.class, thr -> Mono.just(INGREDIENTFORM_URL));
    }

    private void updateIngredient(IngredientCommand ingredientCommand, Ingredient ingredient) {
        ingredient.setId(ingredientCommand.getId());
        ingredient.setDescription(ingredientCommand.getDescription());
        ingredient.setAmount(ingredientCommand.getAmount());
    }

    private Mono<Recipe> addNewIngredient(IngredientCommand ingredientCommand, Recipe recipe, Ingredient newIngredient) {
        return unitOfMeasureService.findById(ingredientCommand.getUom().getId())
                .map(uom -> {
                    newIngredient.setUom(uom);
                    recipe.addIngredient(newIngredient);
                    return recipe;
                });
    }

    @GetMapping("recipe/{recipeId}/ingredient/{id}/delete")
    public String deleteIngredient(@PathVariable String recipeId,
                                   @PathVariable String id) {

        log.debug("deleting ingredient id:" + id);
        ingredientService.deleteById(recipeId, id).block();

        return "redirect:/recipe/" + recipeId + "/ingredients";
    }

    @ModelAttribute("uomList")
    public Flux<UnitOfMeasureCommand> populateUomList() {
        return unitOfMeasureService.listAllUoms();
    }
}
