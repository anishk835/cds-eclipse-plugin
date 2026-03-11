# CDS Plugin - Navigation Not Working - Solutions

## Problem
F3 navigation does nothing in CDS files.

## Root Cause
The plugin is NOT properly installed or loaded in Eclipse.

## Solution 1: Install as Plugin (Recommended for Use)

### Step 1: Build the plugin
```bash
cd /Users/I546280/cds-eclipse-plugin
./build.sh clean install -DskipTests
```

### Step 2: Close Eclipse completely

### Step 3: Install the plugin
1. Start Eclipse
2. **Help → Install New Software...**
3. Click **Add...**
4. Name: `CDS Plugin Local`
5. Location: **COPY THIS EXACT PATH:**
   ```
   file:/Users/I546280/cds-eclipse-plugin/releng/org.example.cds.p2/target/repository
   ```
6. Click **Add**
7. Check the box next to "CDS Feature"
8. Click **Next**, then **Finish**
9. Accept the license
10. **Restart Eclipse** when prompted

### Step 4: Verify installation
1. **Help → About Eclipse → Installation Details → Plug-ins tab**
2. Search for "cds"
3. Should see:
   - `org.example.cds` (version 1.0.0)
   - `org.example.cds.ui` (version 1.0.0)
   - `org.example.cds.ide` (version 1.0.0)

### Step 5: Test navigation
1. Open a `.cds` file
2. Press F3 on a reference
3. Should navigate!

---

## Solution 2: Run as Eclipse Application (for Development)

This is better if you're **developing** the plugin.

### Step 1: Import plugin projects
1. **File → Import → General → Existing Projects into Workspace**
2. Browse to: `/Users/I546280/cds-eclipse-plugin`
3. Select these projects:
   - `org.example.cds`
   - `org.example.cds.ide`
   - `org.example.cds.ui`
4. Click **Finish**

### Step 2: Wait for workspace build
Let Eclipse build all projects (may take a few minutes)

### Step 3: Create Run Configuration
1. Right-click on `org.example.cds.ui` project
2. **Run As → Eclipse Application**
3. A **new Eclipse window** will open with your plugin

### Step 4: Test in the new window
1. In the NEW Eclipse window, create/open a `.cds` file
2. Test F3 navigation
3. Should work!

**Note:** Your CDS files must be in the NEW Eclipse window, not the main development window.

---

## Quick Test: Is the CDS Editor Even Active?

1. Open any `.cds` file
2. Look at the editor tab - what does it say?
   - If it says **"CDS Editor"** or shows CDS icon → Editor is loaded
   - If it says **"Text Editor"** → Plugin is NOT loaded
3. Right-click in the editor → context menu
   - Should see CDS-specific options like "Validate"
   - If not → Plugin is NOT loaded
