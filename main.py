import urllib.request as request
from collections import namedtuple
from geopy.distance import geodesic
from geopy.point import Point
import pandas as pd
import matplotlib.pyplot as plt


BASE_URL = "https://overpass-api.de/api/"

BB = namedtuple('BB', "min max")


def fetch_bb(start_point, end_point):
    bounding_box = BB(min=start_point, max=end_point)
    url = f"{BASE_URL}xapi?*[maxspeed=*][bbox={bounding_box.min.longitude:.5f},{bounding_box.min.latitude:.5f},{bounding_box.max.longitude:.5f},{bounding_box.max.latitude:.5f}]"
    print(url)
    try:
        response = request.urlopen(url)
        data = response.read()
        if len(data) < 1024:
            print(data.decode('utf-8'))
        return len(data) / 1024.0
    except Exception as err:
        print(err)
        return -1

# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.01007,51.00636]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.02015,51.01271]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.03023,51.01906]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.04031,51.02542]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.05040,51.03177]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.06049,51.03812]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.07058,51.04447]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.08067,51.05082]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.09077,51.05717]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.10087,51.06352]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.11097,51.06986]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.12108,51.07621]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.13118,51.08256]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.14129,51.08890]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.15141,51.09524]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.16152,51.10159]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.17164,51.10793]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.18176,51.11427]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.19189,51.12061]
# https://overpass-api.de/api/xapi?*[maxspeed=*][bbox=14.00000,51.00000,14.20201,51.12695]

#            data_size
# distance
# 1.0        10.282227
# 2.0        13.890625
# 3.0        23.850586
# 4.0        26.805664
# 5.0        43.283203
# 6.0        62.801758
# 7.0        69.412109
# 8.0        81.527344
# 9.0       121.003906
# 10.0      138.713867
# 11.0      145.091797
# 12.0      158.366211
# 13.0      190.994141
# 14.0      208.075195
# 15.0      218.109375
# 16.0      235.815430
# 17.0      258.887695
# 18.0      286.084961
# 19.0      320.554688
# 20.0      408.732422


def main():
    df = pd.DataFrame(columns=['distance', 'data_size'])

    for distance in range(1, 501, 50):
        start_point = Point(51, 14)
        middle_point = geodesic().destination(start_point, bearing=0, distance=distance)
        end_point = geodesic().destination(middle_point, bearing=90, distance=distance)
        data_size = fetch_bb(start_point, end_point)

        df = df.append(
            {'distance': distance, 'data_size': data_size},
            ignore_index=True
        )

    df = df.set_index('distance')
    print(df)

    ax = df.plot()
    ax.set_xlabel('BB-Size in km')
    ax.set_ylabel('Download-Size in Kilobyte')
    plt.show()


if __name__ == "__main__":
    main()
