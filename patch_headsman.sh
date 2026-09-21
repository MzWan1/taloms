#!/bin/bash
sed -i '' -e 's/<td colspan="4" class="text-center py-4 text-muted">/<!-- removed -->/g' src/main/resources/templates/dashboard/headsman.html
sed -i '' -e 's/No pending items requiring attention/<!-- removed -->/g' src/main/resources/templates/dashboard/headsman.html
