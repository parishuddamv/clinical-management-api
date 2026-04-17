#!/bin/bash
# Docker Build Verification Script for Clinical Management System

echo "=========================================="
echo "Docker Images Build Verification"
echo "=========================================="
echo ""

# Count total images
TOTAL_IMAGES=$(docker image ls --format "{{.Repository}}" | grep -c "clinicos")
echo "✅ Total ClinicOS Images: $TOTAL_IMAGES"
echo ""

# List all images
echo "📦 Docker Images Created:"
echo "=========================================="
docker image ls --format "{{.Repository}}:{{.Tag}} ({{.Size}})" | grep clinicos | sort | sed 's/^/  /'
echo ""

# Display image details
echo "📋 Image Details:"
echo "=========================================="

SERVICES=("clinic-patient" "clinic-appointment" "clinic-billing" "clinic-followup" "clinic-notification" "clinic-gateway")

for service in "${SERVICES[@]}"; do
    IMAGE_ID=$(docker image ls --format "{{.ID}}" clinicos/$service:latest 2>/dev/null | head -1)
    if [ ! -z "$IMAGE_ID" ]; then
        SIZE=$(docker image ls --format "{{.Size}}" clinicos/$service:latest 2>/dev/null)
        echo "  ✓ clinicos/$service:1.0.0 [$IMAGE_ID] - $SIZE"
    fi
done

echo ""
echo "✅ Docker Build Complete!"
echo "=========================================="
echo ""
echo "To start containers with Docker Compose:"
echo "  docker-compose up -d"
echo ""
echo "To stop and remove containers:"
echo "  docker-compose down"
echo ""

