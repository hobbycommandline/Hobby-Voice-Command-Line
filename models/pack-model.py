#!/usr/bin/python3
"""
package an unzipped model folder as an android obb file

For building for debug I use
./pack-model.py model-en-us/ model-en-us Apache.Alpha.Cephei.2.0.txt --jobb=/HOME/Android/Sdk/tools/bin/jobb --pv 1

We do not encrypt these obb files in the hopes other apps
are able to share them
"""
import uuid
import argparse
import os
import subprocess

def main(args):
    obb_name = args.obb_name
    if not obb_name.endswith(".obb"):
        obb_name += "{pv}.{pn}.obb".format(pv=args.pv, pn=args.pn)
    with open(os.path.join(args.model, "uuid"), "w") as uuid_out:
        uuid_str = str(uuid.uuid4())
        uuid_out.write(uuid_str.upper())
        
    with open(args.license, "r") as license_in:
        with open(os.path.join(args.model, "LICENSE.txt"), "w") as license_out:
            license_out.write(license_in.read())
    subprocess.run(
        [args.jobb, "-d", args.model, "-o", obb_name, "-pn", args.pn, "-pv", args.pv])

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        'model',
        help='the model to pack e.g. ./model-en-us/')
    parser.add_argument(
        'obb_name',
        help='destination of the model e.g. ./model-en-us')
    parser.add_argument(
        'license',
        help='the license of the model to pack e.g. ./Apache.Alpha.Cephei.2.0.txt')
    parser.add_argument(
        '--jobb',
        help='the path of jobb to use',
        default="jobb")
    parser.add_argument(
        '--pn',
        help='package name e.g. org.hobby.activity',
        default="org.hobby.activity")
    parser.add_argument(
        '--pv',
        help='package version e.g. 1')
    args = parser.parse_args()
    main(args)
