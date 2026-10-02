package team.magic.flute.hercules.twelve.labors.constant;

import java.util.*;
import java.util.stream.Collectors;

import static team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormat.*;

/**
 * CMS Data Download Format Compression Codec Enumeration
 *
 * <p>This enumeration defines the supported compression algorithms available for different
 * data export formats within the Hercules business access gateway. It provides format-specific
 * compression options that optimize file size, transfer speed, and storage efficiency for
 * various business data export scenarios.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * compression codecs enable efficient data delivery for business operations by:
 * <ul>
 *   <li>Reducing file sizes for faster data transfer and storage optimization</li>
 *   <li>Supporting different compression algorithms based on format capabilities</li>
 *   <li>Balancing compression ratio with processing performance requirements</li>
 *   <li>Enabling format-specific optimization for various business use cases</li>
 * </ul>
 *
 * <p><strong>Compression Strategy:</strong> Each codec is optimized for specific formats:
 * <ul>
 *   <li>ZSTD - High compression ratio with good performance for text-based formats</li>
 *   <li>SNAPPY - Fast compression/decompression optimized for Parquet analytics</li>
 *   <li>GZIP - Universal compression with broad compatibility</li>
 *   <li>UNCOMPRESSED - No compression for maximum compatibility and speed</li>
 *   <li>BROTLI - Web-optimized compression for modern applications</li>
 *   <li>LZ4/LZ4_RAW - Ultra-fast compression for real-time scenarios</li>
 * </ul>
 *
 * <p><strong>Format Compatibility:</strong> Each compression codec specifies which export
 * formats it supports, ensuring optimal compression selection based on the target format
 * and business requirements.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public enum CmsDownloadFormatCompressionCodec {
    /**
     * Zstandard compression algorithm
     *
     * <p>High-performance compression algorithm that provides excellent compression ratios
     * with good decompression speed. Supports PARQUET, JSON, and CSV formats.
     * Ideal for scenarios where storage efficiency is important.
     */
    ZSTD(PARQUET.name(), JSON.name(), CSV.name()),

    /**
     * Snappy compression algorithm
     *
     * <p>Fast compression algorithm optimized for speed over compression ratio.
     * Primarily used with PARQUET format for analytics workloads where query
     * performance is more important than storage space.
     */
    SNAPPY(PARQUET.name()),

    /**
     * GZIP compression algorithm
     *
     * <p>Widely supported compression algorithm that provides good compression ratios
     * with reasonable performance. Compatible with PARQUET, JSON, and CSV formats.
     * Offers broad compatibility across different systems and tools.
     */
    GZIP(PARQUET.name(), JSON.name(), CSV.name()),

    /**
     * No compression
     *
     * <p>Uncompressed format that provides maximum compatibility and fastest
     * processing speed. Supports all formats: PARQUET, XLSX, JSON, and CSV.
     * Ideal for scenarios where processing speed is critical or compression is not needed.
     */
    UNCOMPRESSED(PARQUET.name(), XLSX.name(), JSON.name(), CSV.name()),

    /**
     * Brotli compression algorithm
     *
     * <p>Modern compression algorithm optimized for web applications and text data.
     * Provides excellent compression ratios for text-based content. Currently
     * supports PARQUET format with potential for expansion to other formats.
     */
    BROTLI(PARQUET.name()),

    /**
     * LZ4 compression algorithm
     *
     * <p>Ultra-fast compression algorithm that prioritizes speed over compression ratio.
     * Ideal for real-time scenarios and high-throughput data processing.
     * Currently optimized for PARQUET format.
     */
    LZ4(PARQUET.name()),

    /**
     * LZ4 Raw compression algorithm
     *
     * <p>Raw variant of LZ4 compression that provides even faster processing
     * with minimal overhead. Designed for scenarios where maximum speed is
     * required. Currently supports PARQUET format.
     */
    LZ4_RAW(PARQUET.name());

    /**
     * Set of supported format names for this compression codec
     */
    final Set<String> supportFormat;

    /**
     * Constructor to initialize supported formats for the compression codec
     *
     * @param args variable arguments representing supported format names
     */
    CmsDownloadFormatCompressionCodec(String...args){
        supportFormat = Arrays.stream(args)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * Get supported compression codecs for a specific export format
     *
     * <p>This method returns all compression codecs that are compatible with the
     * specified export format, enabling the business access gateway to present
     * appropriate compression options based on the selected export format.
     *
     * <p><strong>Business Usage:</strong> Used by the export configuration service
     * to dynamically determine available compression options for different export
     * formats, ensuring that only compatible combinations are offered to users.
     *
     * @param format the export format to check compatibility for
     * @return collection of compatible compression codecs for the specified format
     */
    public static Collection<CmsDownloadFormatCompressionCodec> getSupportCompressCodec(CmsDownloadFormat format){
        return Arrays.stream(CmsDownloadFormatCompressionCodec.values())
                .filter(codec -> codec.supportFormat.contains(format.name().toLowerCase()))
                .collect(Collectors.toList());
    }
}
