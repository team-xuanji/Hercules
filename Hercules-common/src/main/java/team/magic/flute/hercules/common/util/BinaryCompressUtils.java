package team.magic.flute.hercules.common.util;


import io.airlift.compress.Compressor;
import io.airlift.compress.Decompressor;
import io.airlift.compress.lz4.Lz4Compressor;
import io.airlift.compress.lz4.Lz4Decompressor;
import io.airlift.compress.lzo.LzoCompressor;
import io.airlift.compress.lzo.LzoDecompressor;
import io.airlift.compress.snappy.SnappyCompressor;
import io.airlift.compress.snappy.SnappyDecompressor;
import io.airlift.compress.zstd.ZstdCompressor;
import io.airlift.compress.zstd.ZstdDecompressor;
import team.magic.flute.hercules.common.http.HerculesHttpCompressType;

import java.util.Arrays;

public class BinaryCompressUtils {

    private static Compressor getCompressor(HerculesHttpCompressType compressType){
        switch (compressType){
            case LZ4:
                return new Lz4Compressor();
            case LZO:
                return new LzoCompressor();
            case ZSTD:
                return new ZstdCompressor();
            case SNAPPY:
                return new SnappyCompressor();
            default:
                throw  new IllegalArgumentException("Unsupported compress type: " + compressType);
        }
    }

    private static Decompressor getDecompressor(HerculesHttpCompressType compressType){
        switch (compressType){
            case LZ4:
                return new Lz4Decompressor();
            case LZO:
                return new LzoDecompressor();
            case ZSTD:
                return new ZstdDecompressor();
            case SNAPPY:
                return new SnappyDecompressor();
            default:
                throw  new IllegalArgumentException("Unsupported compress type: " + compressType);
        }
    }


    public static byte[] compress(byte[] data, HerculesHttpCompressType compressType){
        Compressor compressor = getCompressor(compressType);
        int maxLength = compressor.maxCompressedLength(data.length);
        byte[] compressed = new byte[maxLength];
        int compressedSize = compressor.compress(data, 0, data.length, compressed, 0, compressed.length);
        return Arrays.copyOf(compressed, compressedSize);
    }

    public static byte[] deCompress(byte[] data, HerculesHttpCompressType compressType,int originalLength){
        if(originalLength == data.length){
            return data;
        }
        Decompressor decompressor = getDecompressor(compressType);
        byte[] uncompressed = new byte[originalLength];
        decompressor.decompress(data, 0, data.length, uncompressed, 0, uncompressed.length);
        return uncompressed;
    }

}
