from dataclasses import dataclass
from typing import Dict, Tuple


@dataclass(frozen=True)
class ZoneTransform:
    """
    Affine transform from a regional-map pixel coordinate
    into the 2048 x 1357 master-map coordinate system.

    master_x = a * regional_x + b * regional_y + c
    master_y = d * regional_x + e * regional_y + f
    """

    zone_id: str
    name: str

    width: int
    height: int

    a: float
    b: float
    c: float

    d: float
    e: float
    f: float

    def regional_to_master(
        self,
        x: float,
        y: float
    ) -> Tuple[float, float]:

        master_x = (
            self.a * x
            + self.b * y
            + self.c
        )

        master_y = (
            self.d * x
            + self.e * y
            + self.f
        )

        return master_x, master_y

    def master_to_regional(
        self,
        master_x: float,
        master_y: float
    ) -> Tuple[float, float]:
        """
        Inverse affine transform.
        """

        px = master_x - self.c
        py = master_y - self.f

        determinant = (
            self.a * self.e
            - self.b * self.d
        )

        if abs(determinant) < 1e-12:
            raise ValueError(
                f"Transform for {self.zone_id} is not invertible."
            )

        regional_x = (
            self.e * px
            - self.b * py
        ) / determinant

        regional_y = (
            -self.d * px
            + self.a * py
        ) / determinant

        return regional_x, regional_y

    def master_bounds(self) -> Tuple[int, int, int, int]:
        """
        Return integer bounding rectangle in master-map pixels.
        """

        corners = (
            self.regional_to_master(0, 0),
            self.regional_to_master(self.width - 1, 0),
            self.regional_to_master(0, self.height - 1),
            self.regional_to_master(
                self.width - 1,
                self.height - 1
            ),
        )

        xs = [p[0] for p in corners]
        ys = [p[1] for p in corners]

        return (
            int(min(xs)),
            int(min(ys)),
            int(max(xs)) + 1,
            int(max(ys)) + 1,
        )


ZONES: Dict[str, ZoneTransform] = {

    # ---------------------------------------------------------
    # WESTEROS
    # ---------------------------------------------------------

    "nw": ZoneTransform(
        zone_id="nw",
        name="North Westeros",

        width=2073,
        height=1966,

        a=0.26598696561824325,
        b=0.00003240432824427481,
        c=19.5523523,

        d=-0.000015556014478764482,
        e=0.2659533495465649,
        f=25.6487478,
    ),

    "sw": ZoneTransform(
        zone_id="sw",
        name="South Westeros",

        width=2067,
        height=1894,

        a=0.24896394753146178,
        b=-0.000013196315372424722,
        c=18.0074268,

        d=0.0000014535087124878994,
        e=0.2488254249207607,
        f=482.4480120,
    ),

    # ---------------------------------------------------------
    # SUMMER ISLES
    # ---------------------------------------------------------

    "si": ZoneTransform(
        zone_id="si",
        name="Summer Isles",

        width=2952,
        height=1714,

        a=0.24890710772551677,
        b=-0.000014289982486865149,
        c=17.8553612,

        d=0.0000142929295154185,
        e=0.2488557861015762,
        f=915.7434305,
    ),

    # ---------------------------------------------------------
    # WESTERN ESSOS
    # ---------------------------------------------------------

    "nwe": ZoneTransform(
        zone_id="nwe",
        name="North-West Essos",

        width=2251,
        height=1945,

        a=0.292637416896,
        b=-0.00000474195061728395,
        c=515.2110972,

        d=0.00004051786311111111,
        e=0.29254803061316875,
        f=175.8865722,
    ),

    "swe": ZoneTransform(
        zone_id="swe",
        name="South-West Essos",

        width=1683,
        height=1228,

        a=0.331419945,
        b=-0.000010275,
        c=496.911538,

        d=-0.000029005,
        e=0.331324618,
        f=733.121605,
    ),

    # ---------------------------------------------------------
    # CENTRAL ESSOS
    # ---------------------------------------------------------

    "ne": ZoneTransform(
        zone_id="ne",
        name="North Essos",

        width=1428,
        height=1515,

        a=0.321896842,
        b=-0.000026834,
        c=1099.512758,

        d=0.000005474,
        e=0.321821671,
        f=271.467371,
    ),

    "se": ZoneTransform(
        zone_id="se",
        name="South Essos",

        width=1645,
        height=1564,

        a=0.321698811,
        b=0.000000142,
        c=1033.633106,

        d=-0.000139931,
        e=0.321974235,
        f=727.704597,
    ),

    # ---------------------------------------------------------
    # EASTERN ESSOS
    # ---------------------------------------------------------

    "nee": ZoneTransform(
        zone_id="nee",
        name="North-East Essos",

        width=1464,
        height=1836,

        a=0.321926267,
        b=-0.000027815,
        c=1558.229390,

        d=0.000002935,
        e=0.321844385,
        f=307.164707,
    ),

    "see": ZoneTransform(
        zone_id="see",
        name="South-East Essos",

        width=1609,
        height=1161,

        a=0.321935107,
        b=-0.000015512,
        c=1527.285771,

        d=-0.000012575,
        e=0.321831998,
        f=900.000856,
    ),

    # ---------------------------------------------------------
    # SOTHORYOS
    # ---------------------------------------------------------

    "so": ZoneTransform(
        zone_id="so",
        name="Sothoryos",

        width=2248,
        height=792,

        a=0.3219377216809079,
        b=0.00014431585335018964,
        c=831.249112,

        d=-0.00004049088072986204,
        e=0.3214243542857143,
        f=1093.184561,
    ),

    # ---------------------------------------------------------
    # ULTHOS
    # ---------------------------------------------------------

    "ul": ZoneTransform(
        zone_id="ul",
        name="Ulthos",

        width=1083,
        height=607,

        a=0.292626437,
        b=0.000017240,
        c=1722.114368,

        d=-0.000054948,
        e=0.292531687,
        f=1172.719604,
    ),
}


ZONE_ORDER = (
    "nw",
    "sw",
    "si",
    "nwe",
    "swe",
    "ne",
    "se",
    "nee",
    "see",
    "so",
    "ul",
)
