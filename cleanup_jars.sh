#!/bin/bash

# Script to remove Android, iOS, and other non-PC/Linux/Mac JAR files
# Keep only Windows, Linux, and Mac (macosx) platform files

JAR_DIR="jogamp-all-platforms/jar"

echo "Cleaning up JOGL JAR files..."
echo "Removing Android, iOS, and other non-desktop platform files..."

# Remove Android-related files
find "$JAR_DIR" -name "*android*" -type f -delete
echo "Removed Android files"

# Remove iOS-related files  
find "$JAR_DIR" -name "*ios*" -type f -delete
echo "Removed iOS files"

# Remove source ZIP files (not needed for runtime)
find "$JAR_DIR" -name "*.zip" -type f -delete
echo "Removed source ZIP files"

# Remove test JAR files (not needed for basic functionality)
find "$JAR_DIR" -name "*test*.jar" -type f -delete
echo "Removed test JAR files"

# Remove debug JAR files
find "$JAR_DIR" -name "*dbg*.jar" -type f -delete
echo "Removed debug JAR files"

# Remove mobile-specific files (keep desktop versions)
find "$JAR_DIR" -name "*mobile*.jar" -type f -delete
echo "Removed mobile JAR files"

# Remove noawt versions (keep standard versions)
find "$JAR_DIR" -name "*noawt*.jar" -type f -delete
echo "Removed noawt JAR files"

# Remove SWT versions (not needed for AWT-based applications)
find "$JAR_DIR" -name "*swt*.jar" -type f -delete
echo "Removed SWT JAR files"

# Remove JavaFX versions
find "$JAR_DIR" -name "*javafx*.jar" -type f -delete
echo "Removed JavaFX JAR files"

# Remove specific driver files that are not needed
find "$JAR_DIR" -name "*bcm*.jar" -type f -delete
find "$JAR_DIR" -name "*egl*.jar" -type f -delete
find "$JAR_DIR" -name "*drm*.jar" -type f -delete
find "$JAR_DIR" -name "*kd*.jar" -type f -delete
find "$JAR_DIR" -name "*intelgdl*.jar" -type f -delete
echo "Removed specific driver files"

echo "Cleanup complete!"
echo "Remaining files support Windows, Linux, and Mac platforms only." 