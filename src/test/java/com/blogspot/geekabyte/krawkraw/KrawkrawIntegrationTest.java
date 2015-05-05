package com.blogspot.geekabyte.krawkraw;

import com.blogspot.geekabyte.krawkraw.interfaces.KrawlerAction;
import com.blogspot.geekabyte.krawkraw.interfaces.callbacks.KrawlerExitCallback;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.runners.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class KrawkrawIntegrationTest {

    private final String host = "http://localhost:" + TestServer.HTTP_PORT;
    TestServer testServer;

    @Before
    public void  startServer() throws Exception {
        testServer = new TestServer();
        testServer.start();
    }

    @After
    public void shutDownServer() throws Exception {
        testServer.shutDown();
    }
    
    @Test
    public void test_extractAllFromUrl() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/mocksite/index.html");

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_exclude_url() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);
        Set<String> urlToExclude = new HashSet<>();
        urlToExclude.add("http://localhost:50036/mocksitetestexclude/three.html");
        urlToExclude.add("http://localhost:50036/mocksitetestexclude/one.html");

        krawkrawSUT.setDelay(0);
        krawkrawSUT.setExcludeURLs(urlToExclude);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/mocksitetestexclude/index.html");

        assertEquals(hrefs.size(), 2);
        verify(mockAction, times(2)).execute(any(FetchedPage.class));
    }

    @Test
    public void test_extractAllFromUrl_Asyncronously() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));

    }
    
    @Test
    public void test_on_exit_callback() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        KrawlerExitCallback mockCallBack = mock(KrawlerExitCallback.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.onExit(mockCallBack);
        krawkrawSUT.setDelay(0);
        // System under test
        Future<Set<String>> futureHrefs = krawkrawSUT.doKrawlAsync(host + "/mocksite/index.html");

        Set<String> hrefs = futureHrefs.get();

        assertEquals(hrefs.size(), 6);
        verify(mockAction, times(6)).execute(any(FetchedPage.class));
        verify(mockCallBack).callBack(anySet());

    }
    
    @Test
    public void test_extractBrokenLink() throws Exception {
        KrawlerAction mockAction = mock(KrawlerAction.class);
        Krawkraw krawkrawSUT = new Krawkraw(mockAction);

        krawkrawSUT.setDelay(0);
        // System under test
        Set<String> hrefs = krawkrawSUT.doKrawl(host + "/brokenlink/index.html");

        ArgumentCaptor<FetchedPage> captor = ArgumentCaptor.forClass(FetchedPage.class);
        assertEquals(hrefs.size(), 5);
        verify(mockAction, times(5)).execute(captor.capture());
        List<FetchedPage> fetchedPages = captor.getAllValues();
        
        int notFoundCount = 0;
        for (FetchedPage page: fetchedPages) {
            if (page.getStatus() == 404) {
                notFoundCount++;
            }
        }
        assertEquals(notFoundCount, 3);
    }
}