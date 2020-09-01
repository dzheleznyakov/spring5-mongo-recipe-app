package guru.springframework.repositories.reactive;

import guru.springframework.domain.Category;
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
public class CategoryReactiveRepositoryTest {
    @Autowired
    CategoryReactiveRepository categoryReactiveRepository;

    @Before
    public void setUp() {
        categoryReactiveRepository.deleteAll().block();
    }

    @Test
    public void testAddCategory() {
        long countBefore = categoryReactiveRepository.count().block();

        Category category = new Category();
        category.setDescription("test description");

        Category savedCategory = categoryReactiveRepository.save(category).block();

        long afterCount = categoryReactiveRepository.count().block();
        assertEquals(countBefore + 1L, afterCount);
        assertNotNull(savedCategory);
        assertNotNull(savedCategory.getId());
        assertEquals(category.getDescription(), savedCategory.getDescription());
    }

    @Test
    public void testFindByDescription() {
        Category category = new Category();
        String description = "test description";
        category.setDescription(description);
        categoryReactiveRepository.save(category).block();

        Category fetchedCategory = categoryReactiveRepository.findByDescription(description).block();

        assertNotNull(fetchedCategory);
        assertNotNull(fetchedCategory.getId());
        assertEquals(description, fetchedCategory.getDescription());
    }
}