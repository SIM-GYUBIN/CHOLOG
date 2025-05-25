import { useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { Navigate, useLocation } from "react-router-dom";
import { AppDispatch, RootState } from "../../store/store";
import { getCurrentUser } from "../../store/slices/userSlice";

interface Props {
  children: React.ReactNode;
}

const PrivateRoute = ({ children }: Props) => {
  const dispatch = useDispatch<AppDispatch>();
  const { isLoggedIn, isAuthChecked } = useSelector(
    (state: RootState) => state.user
  );
  const location = useLocation();

  useEffect(() => {
    if (!isAuthChecked) {
      dispatch(getCurrentUser());
    }
  }, [dispatch, isAuthChecked]);

  if (!isAuthChecked) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--bg)]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-lime-500 mx-auto mb-4"></div>
          <h2 className="text-lg font-semibold text-[var(--text)] mb-2">로딩중...</h2>
          <p className="text-sm text-gray-500">잠시만 기다려주세요</p>
        </div>
      </div>
    );
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export default PrivateRoute;
