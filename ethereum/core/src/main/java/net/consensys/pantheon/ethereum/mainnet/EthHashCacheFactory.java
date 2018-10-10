package net.consensys.pantheon.ethereum.mainnet;

import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.primitives.Ints;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EthHashCacheFactory {

  private static final Logger LOG = LogManager.getLogger();

  public static class EthHashDescriptor {
    private final long datasetSize;
    private final int[] cache;

    public EthHashDescriptor(final long datasetSize, final int[] cache) {
      this.datasetSize = datasetSize;
      this.cache = cache;
    }

    public long getDatasetSize() {
      return datasetSize;
    }

    public int[] getCache() {
      return cache;
    }
  }

  Cache<Long, EthHashDescriptor> descriptorCache = CacheBuilder.newBuilder().maximumSize(5).build();

  public EthHashDescriptor ethHashCacheFor(final long blockNumber) {
    final Long epochIndex = EthHash.epoch(blockNumber);
    try {
      return descriptorCache.get(epochIndex, () -> createHashCache(epochIndex, blockNumber));
    } catch (final ExecutionException ex) {
      throw new RuntimeException("Failed to create a suitable cache for EthHash calculations.", ex);
    }
  }

  private EthHashDescriptor createHashCache(final long epochIndex, final long blockNumber) {
    final int[] cache =
        EthHash.mkCache(Ints.checkedCast(EthHash.cacheSize(epochIndex)), blockNumber);
    return new EthHashDescriptor(EthHash.datasetSize(epochIndex), cache);
  }
}