package guru.springframework.controllers;

import guru.springframework.domain.Recipe;
import guru.springframework.services.RecipeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jt on 6/17/17.
 */
@RunWith(SpringRunner.class)
@WebFluxTest
@Import(IndexController.class)
public class IndexControllerTest {
    @Autowired
    ApplicationContext applicationContext;

    @MockBean
    RecipeService recipeService;

    @Mock
    Model model;

    @Autowired
    IndexController controller;

    private WebTestClient webTestClient;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

//        controller = new IndexController(recipeService);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    public void testMockMVC() throws Exception {
        when(recipeService.getRecipes())
                .thenReturn(Flux.empty());

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody();

//        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
//
//        mockMvc.perform(get("/"))
//                .andExpect(status().isOk())
//                .andExpect(view().name("index"));
    }

    @Test
    public void getIndexPage() {

        //given
        Recipe recipe = new Recipe();
        recipe.setId("1");

        when(recipeService.getRecipes()).thenReturn(Flux.just(new Recipe(), recipe));

        ArgumentCaptor<List<Recipe>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        //when
        String viewName = controller.getIndexPage(model);


        //then
        assertEquals("index", viewName);
        verify(recipeService, times(1)).getRecipes();
        verify(model, times(1)).addAttribute(eq("recipes"), argumentCaptor.capture());
        List<Recipe> setInController = argumentCaptor.getValue();
        assertEquals(2, setInController.size());
    }

}