#!/bin/bash

# 1. Stop tracking user-specific Xcode state
git rm -r --cached *.xcuserstate 2>/dev/null
git rm -r --cached *.xcuserdatad 2>/dev/null

# 2. Stop tracking build artifacts and derived data
git rm -r --cached DerivedData/ 2>/dev/null
git rm -r --cached build/ 2>/dev/null

# 3. Stop tracking sensitive files (if they exist)
git rm -r --cached .env 2>/dev/null

# 4. Finalize the cleanup
echo "Cleanup complete. Now committing the changes..."
git commit -m "chore: remove Xcode user state and build artifacts from tracking"

echo "Done! These files are now ignored and removed from your Git history."
