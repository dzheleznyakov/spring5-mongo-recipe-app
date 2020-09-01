package guru.springframework.repositories.reactive;

import guru.springframework.domain.Recipe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@DataMongoTest
public class RecipeReactiveRepositoryTest {
    @Autowired
    RecipeReactiveRepository recipeReactiveRepository;

    @Before
    public void setUp() {
        recipeReactiveRepository.deleteAll().block();
    }

    @Test
    public void testAddRecipe() {
        long countBefore = recipeReactiveRepository.count().block();

        Recipe recipe = new Recipe();
        recipe.setDescription("test description");

        Recipe savedRecipe = recipeReactiveRepository.save(recipe).block();
        long countAfter = recipeReactiveRepository.count().block();

        assertEquals(countBefore + 1, countAfter);
        assertNotNull(savedRecipe);
        assertNotNull(savedRecipe.getId());
        assertEquals(recipe.getDescription(), savedRecipe.getDescription());
    }
}