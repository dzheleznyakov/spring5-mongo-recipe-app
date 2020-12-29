package guru.springframework.controllers;

import guru.springframework.commands.IngredientCommand;
import guru.springframework.commands.UnitOfMeasureCommand;
import guru.springframework.services.IngredientService;
import guru.springframework.services.RecipeService;
import guru.springframework.services.UnitOfMeasureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Objects;

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

    public IngredientController(IngredientService ingredientService, RecipeService recipeService, UnitOfMeasureService unitOfMeasureService) {
        this.ingredientService = ingredientService;
        this.recipeService = recipeService;
        this.unitOfMeasureService = unitOfMeasureService;
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
        return command
                .map(ingredientCommand -> {
                    ingredientCommand.setRecipeId(recipeId);
                    return ingredientCommand;
                })
                .flatMap(ingredientService::saveIngredientCommand)
                .map(savedCommand -> {
                    log.debug("saved recipe id:" + savedCommand.getRecipeId());
                    log.debug("saved ingredient id:" + savedCommand.getId());
                    return "recipe/ingredient/ingredientform";
                })
                .doOnError(thr -> log.error("Error saving ingredient: " + thr))
                .onErrorResume(WebExchangeBindException.class, thr -> Mono.just(INGREDIENTFORM_URL));
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
