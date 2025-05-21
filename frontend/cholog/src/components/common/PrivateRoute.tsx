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
    // 아직 로그인 여부 확인 중
    return <div>Loading...</div>; // 또는 로딩 스피너
  }

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export default PrivateRoute;
