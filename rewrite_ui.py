import os
import shutil

# delete all old UI screens and components
if os.path.exists('app/src/main/java/com/trixxexe/trixxwave/ui/screens'):
    shutil.rmtree('app/src/main/java/com/trixxexe/trixxwave/ui/screens')
if os.path.exists('app/src/main/java/com/trixxexe/trixxwave/ui/components'):
    shutil.rmtree('app/src/main/java/com/trixxexe/trixxwave/ui/components')

os.makedirs('app/src/main/java/com/trixxexe/trixxwave/ui/screens', exist_ok=True)
