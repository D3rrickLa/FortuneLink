package com.laderrco.fortunelink.portfolio.infrastructure.config.limiting;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bucket4j.BucketConfiguration;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.redisson.SlotCallback;
import org.redisson.api.BatchOptions;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.ObjectParams;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.Codec;
import org.redisson.client.protocol.RedisCommand;
import org.redisson.command.CommandAsyncExecutor;
import org.redisson.command.CommandBatchService;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.MasterSlaveEntry;
import org.redisson.connection.ServiceManager;
import org.redisson.liveobject.core.RedissonObjectBuilder;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RateLimitConfigTest {

  private final ApplicationContextRunner disabledRunner = new ApplicationContextRunner().withConfiguration(
          AutoConfigurations.of(RateLimitConfig.class))
      .withPropertyValues("fortunelink.rate-limit.enabled=false");

private final ApplicationContextRunner enabledRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(RateLimitConfig.class))
    .withBean(RedissonClient.class, () -> Mockito.mock(RedissonClient.class))
    .withBean(CommandAsyncExecutor.class, NoOpCommandAsyncExecutor::new);

  @Test
  void shouldNotLoadConfigWhenDisabled() {
    disabledRunner.run(context -> {
      assertThat(context).doesNotHaveBean(RateLimitConfig.class);
    });
  }

  @Test
  void shouldLoadConfigByDefault() {
    enabledRunner.run(context -> {
      assertThat(context).hasSingleBean(RateLimitConfig.class);
      assertThat(context).hasBean("globalBucketConfig");
    });
  }

  @Test
  void verifyGlobalBucketLimits() {
    enabledRunner.withPropertyValues("fortunelink.rate-limit.global.requests-per-minute=10",
        "fortunelink.rate-limit.global.requests-per-hour=100").run(context -> {
      BucketConfiguration config = context.getBean("globalBucketConfig", BucketConfiguration.class);

      assertThat(config.getBandwidths()).hasSize(3);
      assertThat(config.getBandwidths()[0].getCapacity()).isEqualTo(10);
      assertThat(config.getBandwidths()[1].getCapacity()).isEqualTo(100);
      assertThat(config.getBandwidths()[2].getCapacity()).isEqualTo(10000);
    });
  }

  @Test
  void verifySpecificServiceConfigs() {
    enabledRunner.run(context -> {
      BucketConfiguration marketConfig = context.getBean("marketDataPriceConfig",
          BucketConfiguration.class);
      assertThat(marketConfig.getBandwidths()[0].getCapacity()).isEqualTo(30);

      BucketConfiguration csvConfig = context.getBean("csvImportConfig", BucketConfiguration.class);
      assertThat(csvConfig.getBandwidths()[0].getCapacity()).isEqualTo(3);
    });
  }
}

class NoOpCommandAsyncExecutor implements CommandAsyncExecutor {
  // implement methods with minimal/no-op

  @Override
  public CommandAsyncExecutor copy(ObjectParams objectParams) {
    return null;
  }

  @Override
  public CommandAsyncExecutor copy(boolean trackChanges) {
    return null;
  }

  @Override
  public RedissonObjectBuilder getObjectBuilder() {
    return null;
  }

  @Override
  public ConnectionManager getConnectionManager() {
    return null;
  }

  @Override
  public ServiceManager getServiceManager() {
    return null;
  }

  @Override
  public RedisException convertException(ExecutionException e) {
    return null;
  }

  @Override
  public <V> void transfer(CompletionStage<V> future1, CompletableFuture<V> future2) {

  }

  @Override
  public <V> V getNow(CompletableFuture<V> future) {
    return null;
  }

  @Override
  public <V> V get(RFuture<V> future) {
    return null;
  }

  @Override
  public <V> V get(CompletableFuture<V> future) {
    return null;
  }

  @Override
  public <V> V getInterrupted(RFuture<V> future) throws InterruptedException {
    return null;
  }

  @Override
  public <V> V getInterrupted(CompletableFuture<V> future) throws InterruptedException {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeAsync(RedisClient client, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeAsync(byte[] key, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeAsync(ByteBuf key, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(RedisClient client, MasterSlaveEntry entry, Codec codec,
      RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(RedisClient client, String name, Codec codec,
      RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(RedisClient client, byte[] key, Codec codec,
      RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(RedisClient client, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <R> List<CompletableFuture<R>> executeAllAsync(MasterSlaveEntry entry,
      RedisCommand<?> command, Object... params) {
    return List.of();
  }

  @Override
  public <R> List<CompletableFuture<R>> executeAllAsync(RedisCommand<?> command, Object... params) {
    return List.of();
  }

  @Override
  public <R> List<CompletableFuture<R>> writeAllAsync(RedisCommand<?> command, Object... params) {
    return List.of();
  }

  @Override
  public <R> List<CompletableFuture<R>> writeAllAsync(Codec codec, RedisCommand<?> command,
      Object... params) {
    return List.of();
  }

  @Override
  public <R> List<CompletableFuture<R>> readAllAsync(Codec codec, RedisCommand<?> command,
      Object... params) {
    return List.of();
  }

  @Override
  public <R> List<CompletableFuture<R>> readAllAsync(RedisCommand<?> command, Object... params) {
    return List.of();
  }

  @Override
  public <T, R> RFuture<R> evalReadAsync(RedisClient client, String name, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalReadAsync(String key, Codec codec, RedisCommand<T> evalCommandType,
      String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalReadAsync(MasterSlaveEntry entry, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalReadAsync(RedisClient client, MasterSlaveEntry entry, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalReadAsync(ByteBuf key, Codec codec, RedisCommand<T> evalCommandType,
      String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalWriteAsync(String key, Codec codec, RedisCommand<T> evalCommandType,
      String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalWriteAsync(ByteBuf key, Codec codec, RedisCommand<T> evalCommandType,
      String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalWriteNoRetryAsync(String key, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalWriteAsync(MasterSlaveEntry entry, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(byte[] key, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(ByteBuf key, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(String key, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeAsync(String key, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T> RFuture<Void> writeAllVoidAsync(RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeAsync(String key, RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(String key, RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readAsync(MasterSlaveEntry entry, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readRandomAsync(Codec codec, RedisCommand<T> command, Object... params) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readRandomAsync(RedisClient client, Codec codec, RedisCommand<T> command,
      Object... params) {
    return null;
  }

  @Override
  public <V> RFuture<V> pollFromAnyAsync(String name, Codec codec, RedisCommand<?> command,
      long secondsTimeout, String... queueNames) {
    return null;
  }

  @Override
  public ByteBuf encode(Codec codec, Object value) {
    return null;
  }

  @Override
  public ByteBuf encodeMapKey(Codec codec, Object value) {
    return null;
  }

  @Override
  public ByteBuf encodeMapValue(Codec codec, Object value) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> readBatchedAsync(Codec codec, RedisCommand<T> command,
      SlotCallback<T, R> callback, Object... keys) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> writeBatchedAsync(Codec codec, RedisCommand<T> command,
      SlotCallback<T, R> callback, Object... keys) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalWriteBatchedAsync(Codec codec, RedisCommand<T> command,
      String script, List<Object> keys, SlotCallback<T, R> callback) {
    return null;
  }

  @Override
  public <T, R> RFuture<R> evalReadBatchedAsync(Codec codec, RedisCommand<T> command, String script,
      List<Object> keys, SlotCallback<T, R> callback) {
    return null;
  }

  @Override
  public boolean isEvalShaROSupported() {
    return false;
  }

  @Override
  public void setEvalShaROSupported(boolean value) {

  }

  @Override
  public <T> RFuture<T> syncedEvalWithRetry(String key, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T> RFuture<T> syncedEvalNoRetry(String key, Codec codec, RedisCommand<T> evalCommandType,
      String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T> RFuture<T> syncedEvalNoRetry(long timeout, SyncMode syncMode, String key, Codec codec,
      RedisCommand<T> evalCommandType, String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T> RFuture<T> syncedEvalWithRetry(long timeout, SyncMode syncMode, String key,
      Codec codec, RedisCommand<T> evalCommandType, String script, List<Object> keys,
      Object... params) {
    return null;
  }

  @Override
  public <T> RFuture<T> syncedEval(String key, Codec codec, RedisCommand<T> evalCommandType,
      String script, List<Object> keys, Object... params) {
    return null;
  }

  @Override
  public <T> CompletionStage<T> handleNoSync(CompletionStage<T> stage,
      Function<Throwable, CompletionStage<?>> supplier) {
    return null;
  }

  @Override
  public boolean isTrackChanges() {
    return false;
  }

  @Override
  public CommandBatchService createCommandBatchService(BatchOptions options) {
    return null;
  }

  // implement ONLY what's required (or throw)
}