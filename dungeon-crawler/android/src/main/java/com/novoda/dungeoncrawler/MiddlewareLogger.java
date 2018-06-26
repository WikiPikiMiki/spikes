package com.novoda.dungeoncrawler;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import com.squareup.moshi.Types;
import com.yheriatovych.reductor.Dispatcher;
import com.yheriatovych.reductor.Middleware;
import com.yheriatovych.reductor.Store;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MiddlewareLogger implements Middleware<Redux.GameState> {

    private final List<String> frames;
    private final List<List<String>> historyOfGames;

    private Stage lastStage;

    MiddlewareLogger() {
        this.frames = new ArrayList<>();
        this.historyOfGames = new ArrayList<>();
    }

    @Override
    public Dispatcher create(Store<Redux.GameState> store, Dispatcher nextDispatcher) {
        return action -> {
            Redux.GameState state = store.getState();
//            new Thread(() -> jsonize(state, lastStage)).start();
            jsonize(state, lastStage);
            nextDispatcher.dispatch(action);
            lastStage = state.stage;
        };
    }

    private final Moshi moshi = new Moshi.Builder()
            .add(new GameStateAdapter())
            .build();
    private final JsonAdapter<Redux.GameState> adapter = moshi.adapter(Redux.GameState.class);
    private final Type type = Types.newParameterizedType(List.class, String.class);
    private final JsonAdapter<List<String>> framesAdapter = moshi.adapter(type);

    // Thread this
    private void jsonize(Redux.GameState state, Stage lastStage) {
        Stage stage = state.stage;
        if (!stage.equals(lastStage)) {
            if (stage == Stage.GAME_OVER || stage == Stage.SCREENSAVER) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference gamerTag = database.getReference("gamerTag");
                long timeStamp = System.currentTimeMillis();
                database.getReference("games/" + timeStamp + "/frames")
                        .setValue(framesAdapter.toJson(frames));
                database.getReference("games/" + timeStamp + "/gamerTag")
                        .setValue(gamerTag);
                historyOfGames.add(frames);
                frames.clear();
            }
        }

        String json = adapter.toJson(state);
//        System.out.println("XXX " + json);
        frames.add(json);
    }

    static class GameStateAdapter {

        // GameStateJson is the ideal way to store it
        // GameState is our performance class with the static enemy collections

        @FromJson
        Redux.GameState gameStateFromJson(GameStateJson gameStateJson) {
            Redux.GameState gameState = new Redux.GameState();

            // I'm worried about threading...
            // this is another thread, so the static collection
            // could be read before we have pasted to it
            Redux.GameState.PARTICLE_POOL.addAll(gameStateJson.particlePool);
            Redux.GameState.ENEMY_POOL.addAll(gameStateJson.enemyPool);
            Redux.GameState.ENEMY_SPAWNER_POOL.addAll(gameStateJson.enemySpawnerPool);
            Redux.GameState.LAVA_POOL.addAll(gameStateJson.lavaPool);
            Redux.GameState.CONVEYOR_POOL.addAll(gameStateJson.conveyorPool);

            gameState.boss = gameStateJson.boss;
            gameState.frameTime = gameStateJson.frameTime;
            gameState.lastInputTime = gameStateJson.lastInputTime;
            gameState.attackStartedTime = gameStateJson.attackStartedTime;
            gameState.attacking = gameStateJson.attacking;
            gameState.levelNumber = gameStateJson.levelNumber;
            gameState.playerPositionModifier = gameStateJson.playerPositionModifier;
            gameState.playerPosition = gameStateJson.playerPosition;
            gameState.stageStartTime = gameStateJson.stageStartTime;
            gameState.stage = gameStateJson.stage;
            gameState.lives = gameStateJson.lives;

            return gameState;
        }

        @ToJson
        GameStateJson gameStateToJson(Redux.GameState gameState) {
            GameStateJson gameStateJson = new GameStateJson();

            // I'm worried about threading...
            // this is another thread, so the static collection
            // could be updated before we have copied it
            gameStateJson.particlePool = copyList(Redux.GameState.PARTICLE_POOL);
            gameStateJson.enemyPool = copyList(Redux.GameState.ENEMY_POOL);
            gameStateJson.enemySpawnerPool = copyList(Redux.GameState.ENEMY_SPAWNER_POOL);
            gameStateJson.lavaPool = copyList(Redux.GameState.LAVA_POOL);
            gameStateJson.conveyorPool = copyList(Redux.GameState.CONVEYOR_POOL);

            gameStateJson.boss = gameState.boss;
            gameStateJson.frameTime = gameState.frameTime;
            gameStateJson.lastInputTime = gameState.lastInputTime;
            gameStateJson.attackStartedTime = gameState.attackStartedTime;
            gameStateJson.attacking = gameState.attacking;
            gameStateJson.levelNumber = gameState.levelNumber;
            gameStateJson.playerPositionModifier = gameState.playerPositionModifier;
            gameStateJson.playerPosition = gameState.playerPosition;
            gameStateJson.stageStartTime = gameState.stageStartTime;
            gameStateJson.stage = gameState.stage;
            gameStateJson.lives = gameState.lives;

            return gameStateJson;
        }

        private <T> List<T> copyList(List<T> enemyPool) {
            List<T> pool = new ArrayList<>();
            pool.addAll(enemyPool);
            return pool;
        }

    }

    static class GameStateJson {

        List<Particle> particlePool;
        List<Enemy> enemyPool;
        List<EnemySpawner> enemySpawnerPool;
        List<Lava> lavaPool;
        List<Conveyor> conveyorPool;

        Boss boss;
        long frameTime;
        long lastInputTime;

        long attackStartedTime;
        boolean attacking;

        int levelNumber;
        int playerPositionModifier;
        int playerPosition;
        long stageStartTime;
        Stage stage;
        int lives;
    }

}
