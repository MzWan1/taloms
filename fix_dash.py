import re
with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/dashboard/application/service/DashboardServiceImpl.java', 'r') as f:
    text = f.read()

text = text.replace('companyService.findAll().size()', 'companyService.countAll()')
text = text.replace('villageService.findAll().size()', 'villageService.findAll().size()') # villageService returns List still

with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/dashboard/application/service/DashboardServiceImpl.java', 'w') as f:
    f.write(text)
