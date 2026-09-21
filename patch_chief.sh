#!/bin/bash
sed -i '' -e 's/<td colspan="5" class="text-center py-4 text-muted">/<!-- removed -->/g' src/main/resources/templates/dashboard/chief.html
sed -i '' -e 's/No pending items requiring attention/<!-- removed -->/g' src/main/resources/templates/dashboard/chief.html
