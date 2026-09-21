import sys

with open("src/main/resources/templates/parcels/create.html", "r") as f:
    content = f.read()

target = """        .point-marker-closed {
            background: #28a745;
        }"""
replacement = """        .point-marker-closed {
            background: #28a745;
        }
        .point-marker-overlap {
            background: #dc3545 !important;
            border-color: #fff !important;
        }
        @keyframes blink-warn {
            0% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.5; transform: scale(1.2); }
            100% { opacity: 1; transform: scale(1); }
        }
        .blink-warning {
            animation: blink-warn 0.8s ease-in-out infinite;
        }"""

content = content.replace(target, replacement)

with open("src/main/resources/templates/parcels/create.html", "w") as f:
    f.write(content)
