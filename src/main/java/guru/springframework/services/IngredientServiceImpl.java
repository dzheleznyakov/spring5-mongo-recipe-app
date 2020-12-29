package guru.springframework.services;

import guru.springframework.commands.IngredientCommand;
import guru.springframework.converters.IngredientCommandToIngredient;
import guru.springframework.converters.IngredientToIngredientCommand;
import guru.springframework.domain.Ingredient;
import guru.springframework.domain.Recipe;
import guru.springframework.domain.UnitOfMeasure;
import guru.springframework.repositories.reactive.RecipeReactiveRepository;
import guru.springframework.repositories.reactive.UnitOfMeasureReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Created by jt on 6/28/17.
 */
@Slf4j
@Service
public class IngredientServiceImpl implements IngredientService {

    private final IngredientToIngredientCommand ingredientToIngredientCommand;
    private final IngredientCommandToIngredient ingredientCommandToIngredient;
    private final RecipeReactiveRepository recipeRepository;
    private final UnitOfMeasureReactiveRepository unitOfMeasureRepository;

    public IngredientServiceImpl(IngredientToIngredientCommand ingredientToIngredientCommand,
                                 IngredientCommandToIngredient ingredientCommandToIngredient,
                                 RecipeReactiveRepository recipeRepository,
                                 UnitOfMeasureReactiveRepository unitOfMeasureRepository
    ) {
        this.ingredientToIngredientCommand = ingredientToIngredientCommand;
        this.ingredientCommandToIngredient = ingredientCommandToIngredient;
        this.recipeRepository = recipeRepository;
        this.unitOfMeasureRepository = unitOfMeasureRepository;
    }

    @Override
    public Mono<IngredientCommand> findByRecipeIdAndIngredientId(String recipeId, String ingredientId) {
        return recipeRepository.findById(recipeId)
                .map(recipe -> recipe.getIngredientById(ingredientId))
                .filter(Optional::isPresent)
                .map(ingredient -> {
                    IngredientCommand command = ingredientToIngredientCommand.convert(ingredient.get());
                    command.setRecipeId(recipeId);
                    return command;
                });
    }

    @Override
    @Transactional
    public Mono<IngredientCommand> saveIngredientCommand(IngredientCommand command) {
//        return recipeRepository.findById(command.getRecipeId())
//                .map(recipe -> recipe.getIngredientById(command.getId()))
//                .filter(Optional::isPresent)
//                .map(Optional::get)
//                .zipWith(unitOfMeasureRepository.findById(command.getUom().getId()))
//                .map(tuple -> {
//                    Ingredient ingredientFound = tuple.getT1();
//                    UnitOfMeasure uom = tuple.getT2();
//                    ingredientFound.setDescription(command.getDescription());
//                    ingredientFound.setAmount(command.getAmount());
//                    ingredientFound.setUom(uom);
//                    return ingredientFound;
//                })
//                .switchIfEmpty(Mono.just(command)
//                        .map(ingredientCommandToIngredient::convert)
//                        .map(ingredient -> {
//                            recipe
//                        })
//                )

        return recipeRepository.findById(command.getRecipeId())
                .zipWith(unitOfMeasureRepository.findById(command.getUom().getId()))
                .map(tuple -> {
                    Recipe recipe = tuple.getT1();
                    UnitOfMeasure uom = tuple.getT2();

                    Optional<Ingredient> ingredientOptional = recipe.getIngredientById(command.getId());
                    if (ingredientOptional.isPresent()) {
                        Ingredient ingredientFound = ingredientOptional.get();
                        ingredientFound.setDescription(command.getDescription());
                        ingredientFound.setAmount(command.getAmount());
                        ingredientFound.setUom(uom);
                    } else {
                        Ingredient ingredient = ingredientCommandToIngredient.convert(command);
                        recipe.addIngredient(ingredient);
                    }
                    return recipe;
                })
                .flatMap(entity -> {
                    return recipeRepository.save(entity);
                })
                .map(savedRecipe -> {
                    Optional<Ingredient> savedIngredientOptional = savedRecipe.getIngredientById(command.getId());
                    if (!savedIngredientOptional.isPresent()) {
                        savedIngredientOptional = savedRecipe.getIngredients().stream()
                                .filter(recipeIngredients -> recipeIngredients.getDescription().equals(command.getDescription()))
                                .filter(recipeIngredients -> recipeIngredients.getAmount().equals(command.getAmount()))
                                .filter(recipeIngredients -> recipeIngredients.getUom().getId().equals(command.getUom().getId()))
                                .findFirst();
                    }
                    IngredientCommand ingredientCommandSaved = ingredientToIngredientCommand.convert(savedIngredientOptional.get());
                    ingredientCommandSaved.setRecipeId(savedRecipe.getId());
                    return ingredientCommandSaved;
                })
                .switchIfEmpty(Mono.just(new IngredientCommand())
                        .map(ic -> {
                            log.error("Recipe not found for id: " + command.getRecipeId());
                            return ic;
                        })
                );



//        Optional<Recipe> recipeOptional = recipeRepository.findById(command.getRecipeId())
//                .blockOptional();
//
//        if (!recipeOptional.isPresent()) {
//
//            //todo toss error if not found!
//            log.error("Recipe not found for id: " + command.getRecipeId());
//            return Mono.just(new IngredientCommand());
//        } else {
//            Recipe recipe = recipeOptional.get();
//
//            Optional<Ingredient> ingredientOptional = recipe.getIngredientById(command.getId());
//
//            if (ingredientOptional.isPresent()) {
//                Ingredient ingredientFound = ingredientOptional.get();
//                ingredientFound.setDescription(command.getDescription());
//                ingredientFound.setAmount(command.getAmount());
//                ingredientFound.setUom(unitOfMeasureRepository
//                        .findById(command.getUom().getId())
//                        .block());
////                        .orElseThrow(() -> new RuntimeException("UOM NOT FOUND"))); //todo address this
//
//            } else {
//                //add new Ingredient
//                Ingredient ingredient = ingredientCommandToIngredient.convert(command);
//                //  ingredient.setRecipe(recipe);
//                recipe.addIngredient(ingredient);
//            }
//
//            Recipe savedRecipe = recipeRepository.save(recipe)
//                    .block();
//
//            Optional<Ingredient> savedIngredientOptional = savedRecipe.getIngredientById(command.getId());
//
//            //check by description
//            if (!savedIngredientOptional.isPresent()) {
//                //not totally safe... But best guess
//                savedIngredientOptional = savedRecipe.getIngredients().stream()
//                        .filter(recipeIngredients -> recipeIngredients.getDescription().equals(command.getDescription()))
//                        .filter(recipeIngredients -> recipeIngredients.getAmount().equals(command.getAmount()))
//                        .filter(recipeIngredients -> recipeIngredients.getUom().getId().equals(command.getUom().getId()))
//                        .findFirst();
//            }
//
//            //to do check for fail
//            //enhance with id value
//            IngredientCommand ingredientCommandSaved = ingredientToIngredientCommand.convert(savedIngredientOptional.get());
//            ingredientCommandSaved.setRecipeId(recipe.getId());
//
//            return Mono.just(ingredientCommandSaved);
//        }

    }

    @Override
    public Mono<Void> deleteById(String recipeId, String idToDelete) {

        log.debug("Deleting ingredient: " + recipeId + ":" + idToDelete);

        Optional<Recipe> recipeOptional = recipeRepository.findById(recipeId).blockOptional();

        if (recipeOptional.isPresent()) {
            Recipe recipe = recipeOptional.get();
            log.debug("found recipe");

            Optional<Ingredient> ingredientOptional = recipe.getIngredientById(idToDelete);

            if (ingredientOptional.isPresent()) {
                log.debug("found Ingredient");
                Ingredient ingredientToDelete = ingredientOptional.get();
                // ingredientToDelete.setRecipe(null);
                recipe.getIngredients().remove(ingredientOptional.get());
                recipeRepository.save(recipe).block();
            }
        } else {
            log.debug("Recipe Id Not found. Id:" + recipeId);
        }
        return Mono.empty();
    }
}
