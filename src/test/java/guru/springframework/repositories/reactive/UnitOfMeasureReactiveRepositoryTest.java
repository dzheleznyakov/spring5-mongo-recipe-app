package guru.springframework.repositories.reactive;

import guru.springframework.domain.UnitOfMeasure;
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
public class UnitOfMeasureReactiveRepositoryTest {
    @Autowired
    UnitOfMeasureReactiveRepository unitOfMeasureReactiveRepository;

    @Before
    public void setUp() {
        unitOfMeasureReactiveRepository.deleteAll().block();
    }

    @Test
    public void testAddRecipe() {
        long countBefore = unitOfMeasureReactiveRepository.count().block();

        UnitOfMeasure uom = new UnitOfMeasure();
        uom.setDescription("test description");

        UnitOfMeasure savedUom = unitOfMeasureReactiveRepository.save(uom).block();
        long countAfter = unitOfMeasureReactiveRepository.count().block();

        assertEquals(countBefore + 1, countAfter);
        assertNotNull(savedUom);
        assertNotNull(savedUom.getId());
        assertEquals(uom.getDescription(), savedUom.getDescription());
    }
}