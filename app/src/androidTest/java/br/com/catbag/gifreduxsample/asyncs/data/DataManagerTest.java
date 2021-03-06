package br.com.catbag.gifreduxsample.asyncs.data;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import br.com.catbag.gifreduxsample.models.AppState;
import br.com.catbag.gifreduxsample.models.Gif;
import br.com.catbag.gifreduxsample.models.ImmutableAppState;
import br.com.catbag.gifreduxsample.models.ImmutableGif;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 Copyright 26/10/2016
 Felipe Piñeiro (fpbitencourt@gmail.com),
 Nilton Vasques (nilton.vasques@gmail.com) and
 Raul Abreu (raulccabreu@gmail.com)

 Be free to ask for help, email us!

 Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 in compliance with the License. You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under the License
 is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 implied. See the License for the specific language governing permissions and limitations under
 the License.
 **/

@RunWith(AndroidJUnit4.class)
public class DataManagerTest {

    private static final String TAG_APP_STATE = "TAG_APP_STATE";

    @Rule
    public Timeout globalTimeout = Timeout.seconds(30);

    private DataManager mDataManager;

    @Before
    public void setup() {
        mDataManager = new DataManager(getTargetContext());
    }

    @After
    public void cleanup() throws SnappydbException {
        DB db = DBFactory.open(getTargetContext());
        db.destroy();
    }

    @Test
    public void whenAppStateSavedOnceTime() {
        AppState newAppState = buildAppState(1);
        mDataManager.saveAppState(newAppState);
        assertSaveAppState(newAppState);
    }

    @Test
    public void whenAppStateSavedSeveralTime() {
        AppState newAppState = null;
        for (int i = 0; i < 20; i++) {
            newAppState = buildAppState(i);
            mDataManager.saveAppState(newAppState);
        }
        assertSaveAppState(newAppState);
    }

    @Test
    public void whenSavedAppStateIsLoaded() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        AppState appState = buildAppState();
        saveAppState(appState);
        mDataManager.loadAppState(appStateLoaded -> {
            assertEquals(appState, appStateLoaded);
            signal.countDown();
        });
        signal.await();
    }

    @Test
    public void whenUnsavedAppStateIsLoaded() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        mDataManager.loadAppState(appStateLoaded -> {
            assertTrue(appStateLoaded != null);
            signal.countDown();
        });
        signal.await();
    }

    private void assertSaveAppState(AppState appState) {
        while (mDataManager.isSavingAppState()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Log.e(getClass().getSimpleName(), "sleep error", e);
            }
        }
        assertEquals(appState, getAppStateFromDB());
    }

    private AppState getAppStateFromDB() {
        AppState appstate = null;
        try {
            DB db = DBFactory.open(getTargetContext());
            appstate = AppState.fromJson(db.get(TAG_APP_STATE));
            db.close();
        } catch (SnappydbException | IOException e) {
            Log.e(getClass().getSimpleName(), "error", e);
        }
        return appstate;
    }

    private void saveAppState(AppState appState) {
        try {
            DB db = DBFactory.open(getTargetContext());
            db.put(TAG_APP_STATE, appState.toJson());
            db.close();
        } catch (SnappydbException | IOException e) {
            Log.e(getClass().getSimpleName(), "error", e);
        }
    }

    private AppState buildAppState() {
        return ImmutableAppState.builder().build();
    }

    private AppState buildAppState(int amountGifs) {
        return ImmutableAppState.builder().putAllGifs(buildGifs(amountGifs)).build();
    }

    private Map<String, Gif> buildGifs(int amount) {
        Map<String, Gif> gifs = new LinkedHashMap<>();
        for (int i = 0; i < amount; i++) {
            Gif gif = buildGif(i);
            gifs.put(gif.getUuid(), gif);
        }
        return gifs;
    }

    private Gif buildGif(int index) {
        return ImmutableGif.builder()
                .uuid("" + index)
                .title("Title " + index)
                .url("url" + index)
                .status(Gif.Status.values()[index % Gif.Status.values().length])
                .build();
    }

}