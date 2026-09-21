import re
with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/company/application/service/CompanyServiceImpl.java', 'r') as f:
    text = f.read()

# find the last '}' and replace it
text = re.sub(r'\}\s*\}?\s*@Override\s*@Transactional\(readOnly = true\)\s*public long countAll\(\) \{\s*return companyRepository\.count\(\);\s*\}?\s*$', '', text)
# add it properly
text += """
    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return companyRepository.count();
    }
}
"""
with open('/Users/user/Documents/taloms/src/main/java/za/co/taloms/company/application/service/CompanyServiceImpl.java', 'w') as f:
    f.write(text)
