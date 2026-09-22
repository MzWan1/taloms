path = "src/main/java/za/co/taloms/pto/presentation/PTOPageController.java"
with open(path, "r") as f: c = f.read()

import re
def patch_deleted(match):
    return """
            Pageable pageable = PageRequestUtils.toPageable(page, 10);
            Page<PTOResponse> pageObj = ptoService.findDeleted(pageable);
            
            // Note: Since Chiefs/headsmen are scoped, doing this perfectly via DB would require 
            // a custom findDeletedByVillages query. For minimal change, we will assume admins use this mostly, 
            // but let's implement the list filtering just for the scoped user to avoid DB rewrites for a rare admin tab.
            // Actually, we must use server side. The easiest is to use search method with deleted status, 
            // but search doesn't return deleted. 
            // Wait, PTOJpaRepository.findDeleted doesn't accept villages.
            // For now, to fulfill the prompt exactly:
"""

# Let's see the full deletedList first.
