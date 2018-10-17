import constants


class Trace:
    def __init__(self, conf, x, y, w, h):
        self.conf = int(conf)
        self.x = int(x)
        self.y = int(y)
        self.width = int(w)
        self.height = int(h)

    # TODO now has adapted the coordination in a dirty way, should also change the manifest in VRServer
    def update_coord_with_dimension(self, w, h):
        self.x = int(self.x + ((self.width - w) / 2))
        self.y = int(self.y + ((self.height - h) / 2))

        # Fix out of border issue with a brute-force way
        if self.x < 0:
            self.x = constants.FOUR_K_WIDTH + self.x
            if self.x > constants.FOUR_K_WIDTH - w:
                self.x = constants.FOUR_K_WIDTH - w
        elif self.x + w >= constants.FOUR_K_WIDTH:
            self.x = constants.FOUR_K_WIDTH - w
        if self.y < 0:
            self.y = 0
        elif self.y + h >= constants.FOUR_K_HEIGHT:
            self.y = constants.FOUR_K_HEIGHT - h

        self.width = w
        self.height = h

    def __str__(self):
        return str(self.conf) + " " + str(self.x) + "," + str(self.y) + "," + str(self.width) + "," + str(self.height)
