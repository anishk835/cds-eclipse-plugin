# CDS Eclipse Plugin - Installation & Testing Guide

## Current Status

The plugin has been successfully built with:
- ✅ Java 17 compilation (class version 61.0)
- ✅ Bundle activation policy configured
- ✅ Qualified name resolution for navigation

## Problem in Eclipse

Your Eclipse instance is still running with the **old plugin version** (compiled with Java 25).
The log shows errors from before the rebuild, indicating cached classes.

## Installation Steps

### Option 1: Install from P2 Repository (Recommended)

1. **Build the plugin** (if not already done):
   ```bash
   cd /Users/I546280/cds-eclipse-plugin
   ./build.sh clean install -DskipTests
   ```

2. **In Eclipse:**
   - Go to **Help → Install New Software...**
   - Click **Add...** button
   - Set:
     - Name: `CDS Plugin Local`
     - Location: `file:/Users/I546280/cds-eclipse-plugin/releng/org.example.cds.p2/target/repository`
   - Click **Add**
   - Select the **CDS Feature** that appears
   - Click **Next** and follow the installation wizard
   - **Restart Eclipse** when prompted

3. **Clean Eclipse workspace:**
   - After restart, go to **Project → Clean...**
   - Select **Clean all projects**
   - Click **Clean**

### Option 2: Run as Eclipse Application (Development Mode)

1. **Import the plugin projects into Eclipse:**
   - File → Import → Existing Projects into Workspace
   - Browse to `/Users/I546280/cds-eclipse-plugin`
   - Select all plugin projects

2. **Create a Run Configuration:**
   - Right-click on `org.example.cds.ui` project
   - Run As → Eclipse Application
   - This will launch a new Eclipse instance with your plugin

3. **Test navigation** in the new Eclipse instance

### Option 3: Force Clean Installation

If Eclipse is still showing errors:

1. **Close Eclipse completely**

2. **Clear Eclipse caches:**
   ```bash
   # Backup and clear workspace metadata
   cd ~/eclipse-workspace
   rm -rf .metadata/.plugins/org.eclipse.xtext.*
   rm -rf .metadata/.plugins/org.eclipse.pde.core
   ```

3. **Start Eclipse with clean flag:**
   ```bash
   /Applications/Eclipse.app/Contents/MacOS/eclipse -clean
   ```

4. **Reinstall the plugin** (Option 1 above)

## Verification

After installation, test navigation:

1. **Open a CDS file** (e.g., your annotation file)
2. **Test qualified reference navigation:**
   - Place cursor on `CommonConfigurationService.S4Mapping`
   - Press **F3** or right-click → **Open Declaration**
   - Should navigate to the definition

3. **Check Error Log:**
   - Window → Show View → Error Log
   - Should see NO errors about `class file version 69.0`
   - Should see NO errors about bundle activation

## Troubleshooting

### If you still see "class file version 69.0" errors:

The plugin JAR in Eclipse is stale. You need to:
1. **Uninstall the old plugin**:
   - Help → About Eclipse → Installation Details
   - Find "CDS" plugin
   - Select and click **Uninstall**
   - Restart Eclipse
2. **Reinstall** using Option 1 above

### If navigation still doesn't work:

1. Check that the plugin is actually loaded:
   ```
   Help → About Eclipse → Installation Details → Plug-ins tab
   Search for "cds"
   Should show: org.example.cds, org.example.cds.ui, org.example.cds.ide
   ```

2. Enable Xtext tracing (for debugging):
   - Create file: `~/eclipse-workspace/.options`
   - Add content:
     ```
     org.eclipse.xtext/debug=true
     org.eclipse.xtext/debug/scoping=true
     ```
   - Restart Eclipse with: `eclipse -debug`

## Current Build Artifacts

The correctly compiled plugin is at:
```
/Users/I546280/cds-eclipse-plugin/releng/org.example.cds.p2/target/repository/
```

Verify class version:
```bash
javap -v /Users/I546280/cds-eclipse-plugin/plugins/org.example.cds/target/classes/org/example/cds/scoping/CDSQualifiedNameProvider.class | grep "major version"
# Should show: major version: 61
```
