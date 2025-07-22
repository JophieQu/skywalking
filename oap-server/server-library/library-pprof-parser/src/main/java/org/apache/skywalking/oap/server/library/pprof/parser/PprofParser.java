package org.apache.skywalking.oap.server.library.pprof.parser;

import com.google.perftools.profiles.ProfileProto;
import entity.FrameTree;
import entity.FrameTreeBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Pprof parser for SkyWalking integration.
 * Parses pprof protobuf format files and converts them to frame trees.
 */
public class PprofParser {
    

    /**
     * Parse pprof from byte array
     */
    public static Map<String, FrameTree> parsePprofFile(ByteBuffer buf) throws IOException {
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        
        Map<String, FrameTree> result = new HashMap<>();
        
        try (InputStream stream = new java.io.ByteArrayInputStream(bytes)) {
            // Try to detect if it's gzipped
            InputStream inputStream = isGzippedBytes(bytes) ? new GZIPInputStream(stream) : stream;
            
            ProfileProto.Profile profile = ProfileProto.Profile.parseFrom(inputStream);
            String eventType = determineEventType(profile);
            FrameTree tree = new FrameTreeBuilder(profile).build();
            result.put(eventType, tree);
            
        }
        
        return result;
    }


    /**
     * Parse pprof file and return frame trees for different event types.
     * 
     * @param filePath path to the pprof file
     * @return map of event type to frame tree
     * @throws IOException if file cannot be read
     */
    public static Map<String, FrameTree> parsePprofFile(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Pprof file not found: " + filePath);
        }
        
        Map<String, FrameTree> result = new HashMap<>();
        
        try (InputStream fileStream = new FileInputStream(file)) {
            // Try to detect if it's gzipped
            try (InputStream stream = filePath.endsWith(".gz") || isGzipped(file) ? 
                 new GZIPInputStream(fileStream) : fileStream) {
                
                ProfileProto.Profile profile = ProfileProto.Profile.parseFrom(stream);
                
                String eventType = determineEventType(profile);
                
                // Build frame tree from pprof
                FrameTree tree = new FrameTreeBuilder(profile).build();
                result.put(eventType, tree);
                
            }
        } catch (FileNotFoundException e) {
            throw new IOException("Pprof file not found: " + filePath, e);
        }
        
        return result;
    }
    
    /**
     * Determine the event type based on the profile sample types
     */
    private static String determineEventType(ProfileProto.Profile profile) {
        if (profile.getSampleTypeCount() > 0) {
            ProfileProto.ValueType sampleType = profile.getSampleType(0);
            String typeStr = profile.getStringTable((int) sampleType.getType());
            
            // Map common pprof sample types to event names
            if ("cpu".equals(typeStr) || "samples".equals(typeStr)) {
                return "cpu";
            } else if ("alloc_objects".equals(typeStr) || "alloc_space".equals(typeStr)) {
                return "heap";
            } else if ("contentions".equals(typeStr)) {
                return "block";
            } else if ("delay".equals(typeStr)) {
                return "mutex";
            } else {
                // Default to cpu if unknown
                return "cpu";
            }
        }
        return "cpu"; // default
    }
    
    /**
     * Check if file is gzipped by reading magic bytes
     */
    private static boolean isGzipped(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] magic = new byte[2];
            if (fis.read(magic) == 2) {
                return (magic[0] == (byte) 0x1f) && (magic[1] == (byte) 0x8b);
            }
        }
        return false;
    }

    /**
     * Check if byte array is gzipped by checking magic bytes
     */
    private static boolean isGzippedBytes(byte[] bytes) {
        return bytes.length >= 2 && 
               (bytes[0] == (byte) 0x1f) && (bytes[1] == (byte) 0x8b);
    }
} 