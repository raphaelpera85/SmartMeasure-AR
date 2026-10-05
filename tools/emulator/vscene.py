#!/usr/bin/env python3
"""Move a câmera da cena virtual (virtualscene) do emulador via gRPC, sem janela.

Requer: emulador iniciado com `-grpc <porta>` (start-avd.sh faz isso com SM_GRPC_PORT) e um Python com
grpcio + grpcio-tools (ex.: `uv venv grpcenv && uv pip install --python grpcenv/Scripts/python.exe grpcio grpcio-tools`).
Os stubs são gerados a partir de $ANDROID_SDK/emulator/lib/emulator_controller.proto num diretório de cache.

Uso:
  vscene.py [--port 8554] get
  vscene.py pos X Y Z          # metros
  vscene.py rot X Y Z          # graus (x = pitch, y = yaw, z = roll)
  vscene.py sweep              # varredura lenta lateral + inclinação, para o ARCore ganhar paralaxe
"""
import argparse
import os
import sys
import tempfile
import time


def load_stubs():
    sdk = os.environ.get("ANDROID_SDK", "C:/Android/Sdk")
    proto_dir = os.path.join(sdk, "emulator", "lib")
    out = os.path.join(tempfile.gettempdir(), "sm_emu_grpc_stubs")
    os.makedirs(out, exist_ok=True)
    if not os.path.exists(os.path.join(out, "emulator_controller_pb2_grpc.py")):
        from grpc_tools import protoc
        import grpc_tools
        inc = os.path.join(os.path.dirname(grpc_tools.__file__), "_proto")
        rc = protoc.main(["protoc", f"-I{proto_dir}", f"-I{inc}", f"--python_out={out}",
                          f"--grpc_python_out={out}", os.path.join(proto_dir, "emulator_controller.proto")])
        if rc != 0:
            sys.exit("falha ao gerar stubs gRPC")
    sys.path.insert(0, out)
    import emulator_controller_pb2 as pb
    import emulator_controller_pb2_grpc as pbg
    return pb, pbg


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=int(os.environ.get("SM_GRPC_PORT", "8554")))
    ap.add_argument("cmd")
    ap.add_argument("vals", nargs="*", type=float)
    args = ap.parse_args()
    pb, pbg = load_stubs()
    import grpc
    stub = pbg.EmulatorControllerStub(grpc.insecure_channel(f"localhost:{args.port}"))
    P = pb.PhysicalModelValue

    def get(t):
        return list(stub.getPhysicalModel(P(target=t)).value.data)

    def put(t, xyz):
        stub.setPhysicalModel(P(target=t, value=pb.ParameterValue(data=xyz)))

    if args.cmd == "get":
        print("pos", get(P.POSITION), "rot", get(P.ROTATION))
    elif args.cmd == "pos":
        put(P.POSITION, args.vals)
    elif args.cmd == "rot":
        put(P.ROTATION, args.vals)
    elif args.cmd == "sweep":
        base_p, base_r = get(P.POSITION), get(P.ROTATION)
        steps = 40
        for i in range(steps + 1):  # desloca 0,6 m para o lado e inclina 25° para baixo, devagar
            f = i / steps
            put(P.POSITION, [base_p[0] + 0.6 * f, base_p[1], base_p[2]])
            put(P.ROTATION, [base_r[0] - 25 * f, base_r[1], base_r[2]])
            time.sleep(0.15)
        print("pos", get(P.POSITION), "rot", get(P.ROTATION))
    else:
        ap.error("comando desconhecido")


if __name__ == "__main__":
    main()
