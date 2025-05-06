package com.clamzo.shippy.behavior;

import java.util.UUID;

public record BlockPosKey(UUID worldUuid, int x, int y, int z) {}